package com.fiap.techchallenge.execution.mensageria.usecase;

import com.fiap.techchallenge.execution.mensageria.adapter.listener.EnvelopeEntrada;

/** Tratamento dos eventos consumidos; todos são idempotentes por eventId. */
public interface EventoEntradaUseCase {

    void processarDoOsExecution(EnvelopeEntrada envelope);

    void processarDoBillingExecution(EnvelopeEntrada envelope);
}