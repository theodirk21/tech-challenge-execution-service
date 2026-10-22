package com.fiap.techchallenge.execution.mensageria.framework.persistence.repository;

import com.fiap.techchallenge.execution.mensageria.framework.persistence.entity.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {

    // Native queries não usam hibernate.default_schema: o schema é explícito.
    @Query(value = """
            SELECT * FROM execution.outbox_event
            WHERE published_at IS NULL
            ORDER BY created_at, id
            LIMIT :limite
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEventEntity> findPendentesForUpdateSkipLocked(@Param("limite") int limite);
}