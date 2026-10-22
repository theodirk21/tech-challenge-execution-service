package com.fiap.techchallenge.execution.mensageria.framework.persistence.repository;

import com.fiap.techchallenge.execution.mensageria.framework.persistence.entity.ProcessedEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEventEntity, UUID> {

    // Se outra transação está inserindo o mesmo event_id, este INSERT espera o commit dela e então não insere nada.
    @Modifying
    @Query(value = """
            INSERT INTO execution.processed_event (event_id, processed_at)
            VALUES (:eventId, :processedAt)
            ON CONFLICT (event_id) DO NOTHING
            """, nativeQuery = true)
    int inserirSeAusente(@Param("eventId") UUID eventId, @Param("processedAt") LocalDateTime processedAt);
}