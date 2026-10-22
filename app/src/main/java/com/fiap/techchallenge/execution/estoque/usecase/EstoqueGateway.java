package com.fiap.techchallenge.execution.estoque.usecase;

import com.fiap.techchallenge.execution.estoque.domain.PecaEstoque;
import com.fiap.techchallenge.execution.estoque.domain.ReservaPeca;

import java.util.List;
import java.util.Optional;

public interface EstoqueGateway {

    PecaEstoque salvarPeca(PecaEstoque peca);

    Optional<PecaEstoque> buscarPecaPorId(Long id);

    /** Trava a linha da peça para reservar/liberar sem perder atualização concorrente. */
    Optional<PecaEstoque> buscarPecaPorIdComLock(Long id);

    List<PecaEstoque> listarPecas();

    ReservaPeca salvarReserva(ReservaPeca reserva);

    List<ReservaPeca> listarReservasPorOsId(Long osId);
}