package com.fiap.techchallenge.execution.mensageria.usecase;

import java.util.UUID;

/** Tratamento dos eventos consumidos; todos são idempotentes por eventId. */
public interface EventoEntradaUseCase {

    void processarOsCriada(UUID eventId, Long osId, String solicitacao, String veiculoPlaca, String veiculoModelo);

    void processarPagamentoConfirmado(UUID eventId, Long osId);

    void processarOsCancelada(UUID eventId, Long osId);

    void processarPagamentoEstornado(UUID eventId, Long osId);
}