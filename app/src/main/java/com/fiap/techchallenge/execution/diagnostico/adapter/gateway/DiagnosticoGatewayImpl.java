package com.fiap.techchallenge.execution.diagnostico.adapter.gateway;

import com.fiap.techchallenge.execution.diagnostico.domain.DiagnosticoItem;
import com.fiap.techchallenge.execution.diagnostico.framework.persistence.entity.DiagnosticoItemEntity;
import com.fiap.techchallenge.execution.diagnostico.framework.persistence.repository.DiagnosticoItemRepository;
import com.fiap.techchallenge.execution.diagnostico.usecase.DiagnosticoGateway;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class DiagnosticoGatewayImpl implements DiagnosticoGateway {

    private final DiagnosticoItemRepository repository;

    public DiagnosticoGatewayImpl(DiagnosticoItemRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public List<DiagnosticoItem> substituirItens(Long osId, List<DiagnosticoItem> itens) {
        repository.deleteByOsId(osId);
        repository.flush();
        return repository.saveAll(itens.stream().map(DiagnosticoItemEntity::from).toList()).stream()
                .map(DiagnosticoItemEntity::toDomain).toList();
    }

    @Override
    public List<DiagnosticoItem> listarPorOsId(Long osId) {
        return repository.findByOsIdOrderById(osId).stream().map(DiagnosticoItemEntity::toDomain).toList();
    }
}