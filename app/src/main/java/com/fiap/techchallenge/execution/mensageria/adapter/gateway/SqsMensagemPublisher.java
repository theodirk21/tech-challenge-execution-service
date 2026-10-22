package com.fiap.techchallenge.execution.mensageria.adapter.gateway;

import com.fiap.techchallenge.execution.mensageria.domain.OutboxEvent;
import com.fiap.techchallenge.execution.mensageria.usecase.MensagemPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.ZoneOffset;

/** Monta o envelope do contrato (EVENTOS.md) e envia para a fila de saída. eventId é o id da linha da outbox. */
@Component
public class SqsMensagemPublisher implements MensagemPublisher {

    private static final int VERSAO = 1;

    private final SqsAsyncClient sqs;
    private final JsonMapper mapper;
    private final String nomeDaFila;
    private volatile String urlDaFila;

    public SqsMensagemPublisher(SqsAsyncClient sqs, JsonMapper mapper, @Value("${execution.sqs.saida}") String nomeDaFila) {
        this.sqs = sqs;
        this.mapper = mapper;
        this.nomeDaFila = nomeDaFila;
    }

    @Override
    public void publicar(OutboxEvent evento) {
        ObjectNode envelope = mapper.createObjectNode();
        envelope.put("eventId", evento.getId().toString());
        envelope.put("type", evento.getType());
        envelope.put("version", VERSAO);
        envelope.put("occurredAt", evento.getCreatedAt().toInstant(ZoneOffset.UTC).toString());
        envelope.put("correlationId", String.valueOf(evento.getAggregateId()));
        envelope.set("payload", mapper.readTree(evento.getPayload()));

        String corpo = mapper.writeValueAsString(envelope);
        sqs.sendMessage(r -> r.queueUrl(urlDaFila()).messageBody(corpo)).join();
    }

    private String urlDaFila() {
        if (urlDaFila == null) {
            urlDaFila = sqs.getQueueUrl(r -> r.queueName(nomeDaFila)).join().queueUrl();
        }
        return urlDaFila;
    }
}