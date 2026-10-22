package com.fiap.techchallenge.execution.fila.usecase;

import com.fiap.techchallenge.execution.diagnostico.domain.DiagnosticoItem;
import com.fiap.techchallenge.execution.diagnostico.domain.TipoItemDiagnostico;
import com.fiap.techchallenge.execution.diagnostico.usecase.DiagnosticoGateway;
import com.fiap.techchallenge.execution.diagnostico.usecase.ItemDiagnosticoInput;
import com.fiap.techchallenge.execution.estoque.usecase.EstoqueUseCase;
import com.fiap.techchallenge.execution.estoque.usecase.ItemReserva;
import com.fiap.techchallenge.execution.estoque.usecase.ResultadoReserva;
import com.fiap.techchallenge.execution.exception.BadRequestException;
import com.fiap.techchallenge.execution.exception.NotFoundException;
import com.fiap.techchallenge.execution.fila.domain.ItemFila;
import com.fiap.techchallenge.execution.fila.domain.StatusFila;
import com.fiap.techchallenge.execution.mensageria.domain.TipoEvento;
import com.fiap.techchallenge.execution.mensageria.usecase.EventoSaidaGateway;
import com.fiap.techchallenge.execution.observabilidade.ExecucaoEventLogger;
import com.fiap.techchallenge.execution.servico.usecase.ServicoUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class FilaInteractor implements FilaUseCase {

    private final FilaGateway filaGateway;
    private final DiagnosticoGateway diagnosticoGateway;
    private final EstoqueUseCase estoque;
    private final ServicoUseCase servicos;
    private final EventoSaidaGateway eventos;
    private final ExecucaoEventLogger logger;
    private final Clock clock;

    public FilaInteractor(FilaGateway filaGateway, DiagnosticoGateway diagnosticoGateway, EstoqueUseCase estoque,
                          ServicoUseCase servicos, EventoSaidaGateway eventos, ExecucaoEventLogger logger,
                          Clock clock) {
        this.filaGateway = filaGateway;
        this.diagnosticoGateway = diagnosticoGateway;
        this.estoque = estoque;
        this.servicos = servicos;
        this.eventos = eventos;
        this.logger = logger;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public ItemFila buscarPorOsId(Long osId) {
        return filaGateway.buscarPorOsId(osId).orElseThrow(() -> naoEncontrado(osId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemFila> listarPorStatus(StatusFila status) {
        return filaGateway.listarPorStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiagnosticoItem> listarDiagnostico(Long osId) {
        return diagnosticoGateway.listarPorOsId(osId);
    }

    @Override
    public ItemFila registrarOsCriada(Long osId, String solicitacao, String veiculoPlaca, String veiculoModelo) {
        return filaGateway.buscarPorOsId(osId).orElseGet(() ->
                filaGateway.salvar(ItemFila.criar(osId, solicitacao, veiculoPlaca, veiculoModelo, agora())));
    }

    @Override
    public ItemFila iniciarDiagnostico(Long osId, Long mecanicoId) {
        ItemFila item = buscarComLock(osId);
        StatusFila anterior = item.getStatus();
        item.iniciarDiagnostico(mecanicoId);
        return salvarRegistrando(item, anterior);
    }

    @Override
    public ItemFila concluirDiagnostico(Long osId, List<ItemDiagnosticoInput> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new BadRequestException("Erro: O diagnóstico deve ter ao menos um item!");
        }
        ItemFila item = buscarComLock(osId);
        List<DiagnosticoItem> diagnostico = itens.stream()
                .map(i -> DiagnosticoItem.criar(osId, i.tipo(), i.refId(), i.quantidade()))
                .toList();
        diagnostico.stream().filter(i -> i.getTipo() == TipoItemDiagnostico.PECA).forEach(i -> {
            if (!estoque.existePeca(i.getRefId())) {
                throw new NotFoundException("Peça de id " + i.getRefId() + " não encontrada no estoque!");
            }
        });

        StatusFila anterior = item.getStatus();
        item.concluirDiagnostico();
        diagnosticoGateway.substituirItens(osId, diagnostico);
        eventos.publicar(TipoEvento.DIAGNOSTICO_CONCLUIDO, osId, Map.of("osId", osId, "itens", diagnostico.stream()
                .map(i -> Map.of("tipo", i.getTipo().name(), "refId", i.getRefId(), "quantidade", i.getQuantidade()))
                .toList()));
        return salvarRegistrando(item, anterior);
    }

    @Override
    public ItemFila confirmarPagamento(Long osId) {
        ItemFila item = buscarComLock(osId);
        List<DiagnosticoItem> diagnostico = diagnosticoGateway.listarPorOsId(osId);
        List<ItemReserva> pecas = diagnostico.stream()
                .filter(i -> i.getTipo() == TipoItemDiagnostico.PECA)
                .map(i -> new ItemReserva(i.getRefId(), i.getQuantidade()))
                .toList();

        StatusFila anterior = item.getStatus();
        ResultadoReserva reserva = estoque.reservarPecas(osId, pecas);
        if (!reserva.sucesso()) {
            item.falhar(agora());
            eventos.publicar(TipoEvento.EXECUCAO_FALHOU, osId,
                    Map.of("osId", osId, "motivo", "SEM_ESTOQUE", "detalhe", reserva.detalhe()));
            return salvarRegistrando(item, anterior);
        }

        item.confirmarPagamento();
        servicos.criarParaOs(osId, diagnostico.stream()
                .filter(i -> i.getTipo() == TipoItemDiagnostico.SERVICO)
                .map(DiagnosticoItem::getRefId).toList());
        eventos.publicar(TipoEvento.ESTOQUE_RESERVADO, osId, Map.of("osId", osId));
        return salvarRegistrando(item, anterior);
    }

    @Override
    public ItemFila iniciarProximaExecucao(Long mecanicoId) {
        ItemFila item = filaGateway.proximoDaFilaComLock()
                .orElseThrow(() -> new NotFoundException("Não há itens na fila de execução!"));
        StatusFila anterior = item.getStatus();
        LocalDateTime agora = agora();
        item.iniciarExecucao(mecanicoId, agora);
        servicos.iniciarDaOs(item.getOsId());
        eventos.publicar(TipoEvento.EXECUCAO_INICIADA, item.getOsId(), Map.of("osId", item.getOsId(),
                "mecanicoId", mecanicoId, "iniciouEm", Instant.now(clock).toString()));
        return salvarRegistrando(item, anterior);
    }

    @Override
    public ItemFila finalizarExecucao(Long osId) {
        ItemFila item = buscarComLock(osId);
        StatusFila anterior = item.getStatus();
        item.finalizar(agora());
        servicos.finalizarDaOs(osId);
        estoque.baixarReservas(osId);
        eventos.publicar(TipoEvento.EXECUCAO_FINALIZADA, osId,
                Map.of("osId", osId, "finalizouEm", Instant.now(clock).toString()));
        return salvarRegistrando(item, anterior);
    }

    @Override
    public ItemFila falharExecucao(Long osId, String motivo, String detalhe) {
        ItemFila item = buscarComLock(osId);
        StatusFila anterior = item.getStatus();
        item.falhar(agora());
        estoque.liberarReservas(osId);
        servicos.cancelarPendentesDaOs(osId);
        eventos.publicar(TipoEvento.EXECUCAO_FALHOU, osId, Map.of("osId", osId,
                "motivo", motivo == null ? "FALHA_NA_EXECUCAO" : motivo, "detalhe", detalhe == null ? "" : detalhe));
        return salvarRegistrando(item, anterior);
    }

    @Override
    public void cancelar(Long osId) {
        ItemFila item = filaGateway.buscarPorOsIdComLock(osId).orElse(null);
        // Sem item, ou já encerrado por falha/cancelamento: mensagem repetida ou fora de ordem, nada a compensar.
        if (item == null || item.getStatus() == StatusFila.CANCELADA || item.getStatus() == StatusFila.FALHOU) {
            return;
        }
        StatusFila anterior = item.getStatus();
        item.cancelar(agora());
        estoque.liberarReservas(osId);
        servicos.cancelarPendentesDaOs(osId);
        salvarRegistrando(item, anterior);
    }

    private ItemFila buscarComLock(Long osId) {
        return filaGateway.buscarPorOsIdComLock(osId).orElseThrow(() -> naoEncontrado(osId));
    }

    private ItemFila salvarRegistrando(ItemFila item, StatusFila anterior) {
        ItemFila salvo = filaGateway.salvar(item);
        if (anterior != salvo.getStatus()) {
            logger.registrarStatusAlterado(salvo.getOsId(), anterior.name(), salvo.getStatus().name(), 0L,
                    Instant.now(clock));
        }
        return salvo;
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(clock);
    }

    private NotFoundException naoEncontrado(Long osId) {
        return new NotFoundException("Item da fila da OS " + osId + " não encontrado!");
    }
}