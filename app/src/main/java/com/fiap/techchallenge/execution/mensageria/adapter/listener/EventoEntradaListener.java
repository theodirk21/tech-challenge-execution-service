package com.fiap.techchallenge.execution.mensageria.adapter.listener;

import com.fiap.techchallenge.execution.exception.BadRequestException;
import com.fiap.techchallenge.execution.mensageria.usecase.EventoEntradaUseCase;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

@Slf4j
@Component
public class EventoEntradaListener {

    private final EventoEntradaUseCase eventos;
    private final JsonMapper mapper;

    public EventoEntradaListener(EventoEntradaUseCase eventos, JsonMapper mapper) {
        this.eventos = eventos;
        this.mapper = mapper;
    }

    @SqsListener("${execution.sqs.os-execution}")
    public void consumirFilaOsExecution(String mensagem) {
        log.info("Mensagem recebida na fila OS_EXECUTION: {}", mensagem);
        EnvelopeEntrada envelope = ler(mensagem);
        eventos.processarDoOsExecution(envelope);
    }

    @SqsListener("${execution.sqs.billing-execution}")
    public void consumirFilaBillingExecution(String mensagem) {
        log.info("Mensagem recebida na fila BILLING_EXECUTION: {}", mensagem);
        EnvelopeEntrada envelope = ler(mensagem);
        eventos.processarDoBillingExecution(envelope);
    }

    private EnvelopeEntrada ler(String mensagem) {
        try {
            var raiz = mapper.readTree(mensagem);
            UUID eventId = UUID.fromString(raiz.path("eventId").asString());
            String type = raiz.path("type").asString();
            var payload = raiz.path("payload");
            
            if (payload.isMissingNode()) {
                throw new BadRequestException("Mensagem sem payload!");
            }
            
            int version = raiz.path("version").asInt();
            String occurredAt = raiz.path("occurredAt").asString();
            String correlationId = raiz.path("correlationId").asString();
            
            return new EnvelopeEntrada(eventId, type, payload, version, occurredAt, correlationId);
        } catch (IllegalArgumentException | tools.jackson.core.JacksonException e) {
            throw new BadRequestException("Mensagem inválida: " + e.getMessage());
        }
    }
}