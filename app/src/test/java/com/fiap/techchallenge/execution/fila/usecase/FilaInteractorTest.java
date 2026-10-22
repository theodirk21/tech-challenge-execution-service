package com.fiap.techchallenge.execution.fila.usecase;

import com.fiap.techchallenge.execution.diagnostico.domain.DiagnosticoItem;
import com.fiap.techchallenge.execution.diagnostico.domain.TipoItemDiagnostico;
import com.fiap.techchallenge.execution.diagnostico.usecase.DiagnosticoGateway;
import com.fiap.techchallenge.execution.diagnostico.usecase.ItemDiagnosticoInput;
import com.fiap.techchallenge.execution.estoque.usecase.EstoqueUseCase;
import com.fiap.techchallenge.execution.estoque.usecase.ResultadoReserva;
import com.fiap.techchallenge.execution.exception.BadRequestException;
import com.fiap.techchallenge.execution.exception.ConflictException;
import com.fiap.techchallenge.execution.exception.NotFoundException;
import com.fiap.techchallenge.execution.fila.domain.ItemFila;
import com.fiap.techchallenge.execution.fila.domain.StatusFila;
import com.fiap.techchallenge.execution.mensageria.usecase.EventoSaidaGateway;
import com.fiap.techchallenge.execution.observabilidade.ExecucaoEventLogger;
import com.fiap.techchallenge.execution.servico.usecase.ServicoUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FilaInteractorTest {

    @Mock FilaGateway filaGateway;
    @Mock DiagnosticoGateway diagnosticoGateway;
    @Mock EstoqueUseCase estoque;
    @Mock ServicoUseCase servicos;
    @Mock EventoSaidaGateway eventos;
    @Mock ExecucaoEventLogger logger;

    FilaInteractor interactor;

    static final Long OS = 10L;
    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-22T14:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        interactor = new FilaInteractor(filaGateway, diagnosticoGateway, estoque, servicos, eventos, logger, CLOCK);
        when(filaGateway.salvar(any())).thenAnswer(i -> i.getArgument(0));
    }

    private ItemFila item(StatusFila status) {
        ItemFila i = new ItemFila(1L, OS, status, 0, null, "freio", "ABC1D23", "Gol",
                LocalDateTime.of(2026, 10, 22, 10, 0), null, null);
        when(filaGateway.buscarPorOsIdComLock(OS)).thenReturn(Optional.of(i));
        when(filaGateway.buscarPorOsId(OS)).thenReturn(Optional.of(i));
        return i;
    }

    private void diagnostico() {
        when(diagnosticoGateway.listarPorOsId(OS)).thenReturn(List.of(
                new DiagnosticoItem(1L, OS, TipoItemDiagnostico.PECA, 7L, 2),
                new DiagnosticoItem(2L, OS, TipoItemDiagnostico.SERVICO, 3L, 1)));
    }

    @Test
    void osCriadaCriaItemAguardandoDiagnostico() {
        when(filaGateway.buscarPorOsId(OS)).thenReturn(Optional.empty());

        ItemFila criado = interactor.registrarOsCriada(OS, "freio", "ABC1D23", "Gol");

        assertThat(criado.getStatus()).isEqualTo(StatusFila.AGUARDANDO_DIAGNOSTICO);
        assertThat(criado.getEntrouEm()).isEqualTo(LocalDateTime.of(2026, 10, 22, 14, 0));
    }

    @Test
    void osCriadaRepetidaNaoDuplica() {
        item(StatusFila.EM_DIAGNOSTICO);

        ItemFila r = interactor.registrarOsCriada(OS, "x", "x", "x");

        assertThat(r.getStatus()).isEqualTo(StatusFila.EM_DIAGNOSTICO);
        verify(filaGateway, never()).salvar(any());
    }

    @Test
    void iniciaDiagnosticoRegistrandoMecanico() {
        item(StatusFila.AGUARDANDO_DIAGNOSTICO);

        ItemFila r = interactor.iniciarDiagnostico(OS, 2L);

        assertThat(r.getStatus()).isEqualTo(StatusFila.EM_DIAGNOSTICO);
        assertThat(r.getMecanicoId()).isEqualTo(2L);
        verify(logger).registrarStatusAlterado(eq(OS), eq("AGUARDANDO_DIAGNOSTICO"), eq("EM_DIAGNOSTICO"), anyLong(), any());
    }

    @Test
    void concluiDiagnosticoSalvaItensEPublicaEvento() {
        item(StatusFila.EM_DIAGNOSTICO);
        when(estoque.existePeca(7L)).thenReturn(true);

        ItemFila r = interactor.concluirDiagnostico(OS, List.of(
                new ItemDiagnosticoInput(TipoItemDiagnostico.PECA, 7L, 2),
                new ItemDiagnosticoInput(TipoItemDiagnostico.SERVICO, 3L, 1)));

        assertThat(r.getStatus()).isEqualTo(StatusFila.AGUARDANDO_PAGAMENTO);
        verify(diagnosticoGateway).substituirItens(eq(OS), anyList());
        verify(eventos).publicar(eq("DIAGNOSTICO_CONCLUIDO"), eq(OS), any());
    }

    @Test
    void diagnosticoVazioOuComPecaInexistenteEhRecusado() {
        item(StatusFila.EM_DIAGNOSTICO);
        when(estoque.existePeca(99L)).thenReturn(false);

        assertThatThrownBy(() -> interactor.concluirDiagnostico(OS, List.of())).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> interactor.concluirDiagnostico(OS, null)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> interactor.concluirDiagnostico(OS,
                List.of(new ItemDiagnosticoInput(TipoItemDiagnostico.PECA, 99L, 1)))).isInstanceOf(NotFoundException.class);
        verify(eventos, never()).publicar(any(), any(), any());
    }

    @Test
    void pagamentoConfirmadoReservaPecasECriaServicos() {
        item(StatusFila.AGUARDANDO_PAGAMENTO);
        diagnostico();
        when(estoque.reservarPecas(eq(OS), anyList())).thenReturn(ResultadoReserva.ok());

        ItemFila r = interactor.confirmarPagamento(OS);

        assertThat(r.getStatus()).isEqualTo(StatusFila.NA_FILA);
        verify(servicos).criarParaOs(OS, List.of(3L));
        verify(eventos).publicar(eq("ESTOQUE_RESERVADO"), eq(OS), any());
    }

    @Test
    void pagamentoConfirmadoSemEstoqueFalhaEPublicaCompensacao() {
        item(StatusFila.AGUARDANDO_PAGAMENTO);
        diagnostico();
        when(estoque.reservarPecas(eq(OS), anyList()))
                .thenReturn(ResultadoReserva.falha("Peça 7: requisitado 2, disponível 0"));

        ItemFila r = interactor.confirmarPagamento(OS);

        assertThat(r.getStatus()).isEqualTo(StatusFila.FALHOU);
        verify(eventos).publicar(eq("EXECUCAO_FALHOU"), eq(OS), eq(Map.of("osId", OS, "motivo", "SEM_ESTOQUE",
                "detalhe", "Peça 7: requisitado 2, disponível 0")));
        verify(servicos, never()).criarParaOs(any(), anyList());
    }

    @Test
    void iniciaProximaExecucao() {
        ItemFila i = new ItemFila(1L, OS, StatusFila.NA_FILA, 0, null, null, null, null,
                LocalDateTime.of(2026, 10, 22, 10, 0), null, null);
        when(filaGateway.proximoDaFilaComLock()).thenReturn(Optional.of(i));

        ItemFila r = interactor.iniciarProximaExecucao(2L);

        assertThat(r.getStatus()).isEqualTo(StatusFila.EM_EXECUCAO);
        assertThat(r.getIniciouEm()).isEqualTo(LocalDateTime.of(2026, 10, 22, 14, 0));
        verify(servicos).iniciarDaOs(OS);
        verify(eventos).publicar(eq("EXECUCAO_INICIADA"), eq(OS), any());
    }

    @Test
    void filaVaziaNaoInicia() {
        when(filaGateway.proximoDaFilaComLock()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> interactor.iniciarProximaExecucao(2L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void finalizaBaixandoReservasEPublicando() {
        item(StatusFila.EM_EXECUCAO);

        ItemFila r = interactor.finalizarExecucao(OS);

        assertThat(r.getStatus()).isEqualTo(StatusFila.FINALIZADA);
        verify(servicos).finalizarDaOs(OS);
        verify(estoque).baixarReservas(OS);
        verify(eventos).publicar(eq("EXECUCAO_FINALIZADA"), eq(OS), any());
    }

    @Test
    void falhaNaExecucaoCompensaEPublica() {
        item(StatusFila.EM_EXECUCAO);

        ItemFila r = interactor.falharExecucao(OS, null, null);

        assertThat(r.getStatus()).isEqualTo(StatusFila.FALHOU);
        verify(estoque).liberarReservas(OS);
        verify(servicos).cancelarPendentesDaOs(OS);
        verify(eventos).publicar(eq("EXECUCAO_FALHOU"), eq(OS), eq(Map.of("osId", OS,
                "motivo", "FALHA_NA_EXECUCAO", "detalhe", "")));
    }

    @Test
    void cancelarLiberaReservasEEncerra() {
        item(StatusFila.NA_FILA);

        interactor.cancelar(OS);

        verify(estoque).liberarReservas(OS);
        verify(servicos).cancelarPendentesDaOs(OS);
        verify(filaGateway).salvar(any());
    }

    @Test
    void cancelarEhIdempotenteParaItemInexistenteCanceladoOuFalhado() {
        when(filaGateway.buscarPorOsIdComLock(OS)).thenReturn(Optional.empty());
        interactor.cancelar(OS);
        item(StatusFila.CANCELADA);
        interactor.cancelar(OS);
        item(StatusFila.FALHOU);
        interactor.cancelar(OS);

        verify(estoque, never()).liberarReservas(any());
    }

    @Test
    void cancelarOsFinalizadaEhConflito() {
        item(StatusFila.FINALIZADA);

        assertThatThrownBy(() -> interactor.cancelar(OS)).isInstanceOf(ConflictException.class);
    }

    @Test
    void consultas() {
        item(StatusFila.NA_FILA);
        when(filaGateway.listarPorStatus(StatusFila.NA_FILA)).thenReturn(List.of());
        when(diagnosticoGateway.listarPorOsId(OS)).thenReturn(List.of());
        when(filaGateway.buscarPorOsId(99L)).thenReturn(Optional.empty());

        assertThat(interactor.buscarPorOsId(OS).getOsId()).isEqualTo(OS);
        assertThat(interactor.listarPorStatus(StatusFila.NA_FILA)).isEmpty();
        assertThat(interactor.listarDiagnostico(OS)).isEmpty();
        assertThatThrownBy(() -> interactor.buscarPorOsId(99L)).isInstanceOf(NotFoundException.class);
    }
}