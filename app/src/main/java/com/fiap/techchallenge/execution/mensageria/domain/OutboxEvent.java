package com.fiap.techchallenge.execution.mensageria.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public class OutboxEvent {

    private final UUID id;
    private final Long aggregateId;
    private final String type;
    private final String payload;
    private final LocalDateTime createdAt;
    private LocalDateTime publishedAt;

    public OutboxEvent(UUID id, Long aggregateId, String type, String payload,
                       LocalDateTime createdAt, LocalDateTime publishedAt) {
        this.id = id;
        this.aggregateId = aggregateId;
        this.type = type;
        this.payload = payload;
        this.createdAt = createdAt;
        this.publishedAt = publishedAt;
    }

    public static OutboxEvent criar(String type, Long aggregateId, String payload, LocalDateTime agora) {
        return new OutboxEvent(UUID.randomUUID(), aggregateId, type, payload, agora, null);
    }

    public void marcarPublicado(LocalDateTime agora) {
        this.publishedAt = agora;
    }

    public boolean foiPublicado() {
        return publishedAt != null;
    }

    public UUID getId() { return id; }
    public Long getAggregateId() { return aggregateId; }
    public String getType() { return type; }
    public String getPayload() { return payload; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
}