package com.fiap.techchallenge.execution.mensageria.adapter.gateway;

import com.fiap.techchallenge.execution.mensageria.domain.OutboxEvent;
import com.fiap.techchallenge.execution.mensageria.usecase.EventoSaidaGateway;
import com.fiap.techchallenge.execution.mensageria.usecase.OutboxGateway;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
public class EventoSaidaGatewayImpl implements EventoSaidaGateway {

    private final OutboxGateway outbox;
    private final JsonMapper mapper;
    private final Clock clock;

    public EventoSaidaGatewayImpl(OutboxGateway outbox, JsonMapper mapper, Clock clock) {
        this.outbox = outbox;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Override
    public void publicar(String tipo, Long osId, Object payload) {
        outbox.salvar(OutboxEvent.criar(tipo, osId, mapper.writeValueAsString(payload), LocalDateTime.now(clock)));
    }
}