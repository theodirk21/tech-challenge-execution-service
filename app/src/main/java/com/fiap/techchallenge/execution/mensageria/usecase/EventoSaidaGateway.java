package com.fiap.techchallenge.execution.mensageria.usecase;

/** Porta para publicar eventos da saga; a implementação grava na outbox, na mesma transação do caso de uso. */
public interface EventoSaidaGateway {

    void publicar(String tipo, Long osId, Object payload);
}