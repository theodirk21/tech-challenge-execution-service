package com.fiap.techchallenge.execution.fila.framework.persistence.repository;

import com.fiap.techchallenge.execution.fila.framework.persistence.entity.ItemFilaEntity;

import com.fiap.techchallenge.execution.fila.domain.StatusFila;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ItemFilaRepository extends JpaRepository<ItemFilaEntity, Long> {

    Optional<ItemFilaEntity> findByOsId(Long osId);

    List<ItemFilaEntity> findByStatusOrderByPrioridadeDescEntrouEmAsc(StatusFila status);

    // Native queries não usam hibernate.default_schema: o schema é explícito.
    @Query(value = "SELECT * FROM execution.fila_execucao WHERE os_id = :osId FOR UPDATE", nativeQuery = true)
    Optional<ItemFilaEntity> findByOsIdForUpdate(@Param("osId") Long osId);

    @Query(value = """
            SELECT * FROM execution.fila_execucao
            WHERE status = 'NA_FILA'
            ORDER BY prioridade DESC, entrou_em ASC
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<ItemFilaEntity> findProximoForUpdateSkipLocked();
}
