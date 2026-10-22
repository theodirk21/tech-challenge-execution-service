package com.fiap.techchallenge.execution.estoque.framework.persistence.repository;

import com.fiap.techchallenge.execution.estoque.framework.persistence.entity.ReservaPecaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservaPecaRepository extends JpaRepository<ReservaPecaEntity, Long> {

    List<ReservaPecaEntity> findByOsIdOrderById(Long osId);
}