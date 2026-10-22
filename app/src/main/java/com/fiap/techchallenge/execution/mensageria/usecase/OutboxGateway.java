package com.fiap.techchallenge.execution.mensageria.usecase;

import com.fiap.techchallenge.execution.mensageria.domain.OutboxEvent;

import java.util.List;

public interface OutboxGateway {

    OutboxEvent salvar(OutboxEvent evento);

    /** Eventos ainda não publicados, em ordem de criação, travados contra outros publicadores (SKIP LOCKED). */
    List<OutboxEvent> buscarPendentesComLock(int limite);
}