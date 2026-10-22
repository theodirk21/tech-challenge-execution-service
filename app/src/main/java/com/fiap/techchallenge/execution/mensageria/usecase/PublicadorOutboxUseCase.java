package com.fiap.techchallenge.execution.mensageria.usecase;

public interface PublicadorOutboxUseCase {

    /** Publica até {@code lote} eventos pendentes, na ordem de criação; devolve quantos foram publicados. */
    int publicarPendentes(int lote);
}