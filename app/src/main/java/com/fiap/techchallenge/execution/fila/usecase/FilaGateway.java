package com.fiap.techchallenge.execution.fila.usecase;

import com.fiap.techchallenge.execution.fila.domain.ItemFila;
import com.fiap.techchallenge.execution.fila.domain.StatusFila;

import java.util.List;
import java.util.Optional;

public interface FilaGateway {

    ItemFila salvar(ItemFila item);

    Optional<ItemFila> buscarPorOsId(Long osId);

    /** Busca com lock de linha para alterar o item sem corrida entre transações. */
    Optional<ItemFila> buscarPorOsIdComLock(Long osId);

    /** Próximo item da fila (maior prioridade, mais antigo), ignorando linhas já travadas por outra transação. */
    Optional<ItemFila> proximoDaFilaComLock();

    List<ItemFila> listarPorStatus(StatusFila status);
}
