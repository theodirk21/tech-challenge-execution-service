package com.fiap.techchallenge.execution.mensageria.adapter.gateway;

import com.fiap.techchallenge.execution.mensageria.domain.OutboxEvent;
import com.fiap.techchallenge.execution.mensageria.usecase.MensagemPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.ZoneOffset;

@Component
public class SnsMensagemPublisher implements MensagemPublisher {

    private static final int VERSAO = 1;

    private final SnsClient sns;
    private final JsonMapper mapper;
    private final String arnDoTopico;

    public SnsMensagemPublisher(SnsClient sns, JsonMapper mapper, @Value("${execution.sns.topico-arn}") String arnDoTopico) {
        this.sns = sns;
        this.mapper = mapper;
        this.arnDoTopico = arnDoTopico;
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
        // O atributo "type" permite que os assinantes filtrem eventos com filter policy
        sns.publish(r -> r.topicArn(arnDoTopico)
                .message(corpo)
                .messageAttributes(java.util.Map.of("type", MessageAttributeValue.builder()
                        .dataType("String").stringValue(evento.getType()).build())));
    }
}
