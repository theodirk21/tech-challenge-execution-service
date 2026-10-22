package com.fiap.techchallenge.execution.servico.framework.persistence.repository;

import com.fiap.techchallenge.execution.servico.framework.persistence.entity.ServicoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServicoRepository extends JpaRepository<ServicoEntity, Long> {

    List<ServicoEntity> findByOsIdOrderById(Long osId);
}