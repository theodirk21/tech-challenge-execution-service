package com.fiap.techchallenge.execution.mensageria.adapter.listener;

import com.fiap.techchallenge.execution.exception.BadRequestException;
import com.fiap.techchallenge.execution.mensageria.usecase.EventoEntradaUseCase;
import com.fiap.techchallenge.execution.observabilidade.EventoLogger;
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
    private final EventoLogger eventoLogger;

    public EventoEntradaListener(EventoEntradaUseCase eventos, JsonMapper mapper, EventoLogger eventoLogger) {
        this.eventos = eventos;
        this.mapper = mapper;
        this.eventoLogger = eventoLogger;
    }

    @SqsListener("${execution.sqs.os-execution}")
    public void consumirFilaOsExecution(String mensagem) {
        EnvelopeEntrada envelope = ler(mensagem);
        eventoLogger.logRecebimentoMensagem("os-execution",
                envelope.correlationId(),
                String.valueOf(envelope.eventId()),
                envelope.type());

        eventos.processarDoOsExecution(envelope);
    }

    @SqsListener("${execution.sqs.billing-execution}")
    public void consumirFilaBillingExecution(String mensagem) {
        EnvelopeEntrada envelope = ler(mensagem);
        eventoLogger.logRecebimentoMensagem("billing-execution",
                envelope.correlationId(),
                String.valueOf(envelope.eventId()),
                envelope.type());
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