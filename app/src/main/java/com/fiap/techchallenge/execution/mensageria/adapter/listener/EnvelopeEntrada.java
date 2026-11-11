package com.fiap.techchallenge.execution.mensageria.adapter.listener;

import tools.jackson.databind.JsonNode;

import java.util.UUID;

public record EnvelopeEntrada(
    UUID eventId,
    String type,
    JsonNode payload,
    int version,
    String occurredAt,
    String correlationId
) {

    public Long osId() {
        return payload.path("osId").asLong();
    }

    public String texto(String campo) {
        JsonNode no = payload.path(campo);
        return no.isMissingNode() || no.isNull() ? null : no.asString();
    }

    public String texto(String objeto, String campo) {
        JsonNode no = payload.path(objeto).path(campo);
        return no.isMissingNode() || no.isNull() ? null : no.asString();
    }

    public String criadaEm() {
        return texto("criadaEm");
    }

    public Long clienteId() {
        return payload.path("clienteId").asLong();
    }

    public Long veiculoId() {
        return payload.path("veiculoId").asLong();
    }

    public String solicitacao() {
        return texto("solicitacao");
    }
}