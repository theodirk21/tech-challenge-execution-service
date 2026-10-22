package com.fiap.techchallenge.execution.diagnostico.framework.persistence.repository;

import com.fiap.techchallenge.execution.diagnostico.framework.persistence.entity.DiagnosticoItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiagnosticoItemRepository extends JpaRepository<DiagnosticoItemEntity, Long> {

    List<DiagnosticoItemEntity> findByOsIdOrderById(Long osId);

    void deleteByOsId(Long osId);
}