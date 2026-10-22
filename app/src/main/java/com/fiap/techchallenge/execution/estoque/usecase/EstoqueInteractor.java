package com.fiap.techchallenge.execution.estoque.usecase;

import com.fiap.techchallenge.execution.estoque.domain.PecaEstoque;
import com.fiap.techchallenge.execution.estoque.domain.ReservaPeca;
import com.fiap.techchallenge.execution.exception.BadRequestException;
import com.fiap.techchallenge.execution.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@Transactional
public class EstoqueInteractor implements EstoqueUseCase {

    private final EstoqueGateway gateway;

    public EstoqueInteractor(EstoqueGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PecaEstoque> listarPecas() {
        return gateway.listarPecas();
    }

    @Override
    @Transactional(readOnly = true)
    public PecaEstoque buscarPeca(Long id) {
        return gateway.buscarPecaPorId(id).orElseThrow(() -> pecaNaoEncontrada(id));
    }

    @Override
    public PecaEstoque criarPeca(String nome, int quantidade) {
        if (nome == null || nome.isBlank()) {
            throw new BadRequestException("Erro: O nome da peça é obrigatório!");
        }
        if (quantidade < 0) {
            throw new BadRequestException("Erro: A quantidade em estoque não pode ser negativa!");
        }
        return gateway.salvarPeca(new PecaEstoque(null, nome, quantidade));
    }

    @Override
    public PecaEstoque reporEstoque(Long id, int quantidade) {
        PecaEstoque peca = gateway.buscarPecaPorIdComLock(id).orElseThrow(() -> pecaNaoEncontrada(id));
        peca.repor(quantidade);
        return gateway.salvarPeca(peca);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existePeca(Long id) {
        return gateway.buscarPecaPorId(id).isPresent();
    }

    @Override
    public ResultadoReserva reservarPecas(Long osId, List<ItemReserva> itens) {
        // Soma por peça e trava em ordem de id para evitar deadlock entre OS concorrentes.
        Map<Long, Integer> necessario = new TreeMap<>();
        itens.forEach(i -> necessario.merge(i.pecaId(), i.quantidade(), Integer::sum));

        Map<Long, PecaEstoque> travadas = new TreeMap<>();
        for (Map.Entry<Long, Integer> e : necessario.entrySet()) {
            PecaEstoque peca = gateway.buscarPecaPorIdComLock(e.getKey()).orElse(null);
            if (peca == null) {
                return ResultadoReserva.falha("Peça %d: não encontrada no estoque".formatted(e.getKey()));
            }
            if (!peca.temEstoque(e.getValue())) {
                return ResultadoReserva.falha("Peça %d: requisitado %d, disponível %d"
                        .formatted(e.getKey(), e.getValue(), peca.getQuantidade()));
            }
            travadas.put(e.getKey(), peca);
        }

        necessario.forEach((pecaId, qtd) -> {
            PecaEstoque peca = travadas.get(pecaId);
            peca.reservar(qtd);
            gateway.salvarPeca(peca);
            gateway.salvarReserva(ReservaPeca.criar(osId, pecaId, qtd));
        });
        return ResultadoReserva.ok();
    }

    @Override
    public void liberarReservas(Long osId) {
        reservasAtivas(osId).forEach(reserva -> {
            PecaEstoque peca = gateway.buscarPecaPorIdComLock(reserva.getPecaId())
                    .orElseThrow(() -> pecaNaoEncontrada(reserva.getPecaId()));
            peca.liberar(reserva.getQuantidade());
            gateway.salvarPeca(peca);
            reserva.liberar();
            gateway.salvarReserva(reserva);
        });
    }

    @Override
    public void baixarReservas(Long osId) {
        reservasAtivas(osId).forEach(reserva -> {
            reserva.baixar();
            gateway.salvarReserva(reserva);
        });
    }

    private List<ReservaPeca> reservasAtivas(Long osId) {
        return gateway.listarReservasPorOsId(osId).stream()
                .filter(ReservaPeca::estaReservada)
                .sorted(java.util.Comparator.comparing(ReservaPeca::getPecaId))
                .toList();
    }

    private NotFoundException pecaNaoEncontrada(Long id) {
        return new NotFoundException("Peça de id " + id + " não encontrada no estoque!");
    }
}