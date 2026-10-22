package com.fiap.techchallenge.execution.mensageria.usecase;

import com.fiap.techchallenge.execution.mensageria.domain.OutboxEvent;

public interface MensagemPublisher {

    /** Envia o evento ao broker; lança exceção se não for possível, para ele continuar pendente na outbox. */
    void publicar(OutboxEvent evento);
}