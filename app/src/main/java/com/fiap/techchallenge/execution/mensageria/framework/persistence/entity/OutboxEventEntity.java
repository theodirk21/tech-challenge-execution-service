package com.fiap.techchallenge.execution.mensageria.framework.persistence.entity;

import com.fiap.techchallenge.execution.mensageria.domain.OutboxEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_event")
public class OutboxEventEntity {

    @Id
    private UUID id;

    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    @Column(nullable = false, length = 60)
    private String type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String payload;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    protected OutboxEventEntity() {}

    public static OutboxEventEntity from(OutboxEvent d) {
        OutboxEventEntity e = new OutboxEventEntity();
        e.id = d.getId();
        e.aggregateId = d.getAggregateId();
        e.type = d.getType();
        e.payload = d.getPayload();
        e.createdAt = d.getCreatedAt();
        e.publishedAt = d.getPublishedAt();
        return e;
    }

    public OutboxEvent toDomain() {
        return new OutboxEvent(id, aggregateId, type, payload, createdAt, publishedAt);
    }
}