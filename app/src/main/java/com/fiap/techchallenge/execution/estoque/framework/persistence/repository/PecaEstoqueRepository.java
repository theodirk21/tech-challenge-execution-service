package com.fiap.techchallenge.execution.estoque.framework.persistence.repository;

import com.fiap.techchallenge.execution.estoque.framework.persistence.entity.PecaEstoqueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PecaEstoqueRepository extends JpaRepository<PecaEstoqueEntity, Long> {

    // Native queries não usam hibernate.default_schema: o schema é explícito.
    @Query(value = "SELECT * FROM execution.peca_estoque WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<PecaEstoqueEntity> findByIdForUpdate(@Param("id") Long id);
}