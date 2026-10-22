package com.fiap.techchallenge.execution.mensageria.adapter.listener;

import tools.jackson.databind.JsonNode;

import java.util.UUID;

public record EnvelopeEntrada(UUID eventId, String type, JsonNode payload) {

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
}