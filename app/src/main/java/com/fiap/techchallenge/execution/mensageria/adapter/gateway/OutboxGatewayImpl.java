package com.fiap.techchallenge.execution.mensageria.adapter.gateway;

import com.fiap.techchallenge.execution.mensageria.domain.OutboxEvent;
import com.fiap.techchallenge.execution.mensageria.framework.persistence.entity.OutboxEventEntity;
import com.fiap.techchallenge.execution.mensageria.framework.persistence.repository.OutboxEventRepository;
import com.fiap.techchallenge.execution.mensageria.usecase.OutboxGateway;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class OutboxGatewayImpl implements OutboxGateway {

    private final OutboxEventRepository repository;

    public OutboxGatewayImpl(OutboxEventRepository repository) {
        this.repository = repository;
    }

    @Override
    public OutboxEvent salvar(OutboxEvent evento) {
        return repository.save(OutboxEventEntity.from(evento)).toDomain();
    }

    @Override
    public List<OutboxEvent> buscarPendentesComLock(int limite) {
        return repository.findPendentesForUpdateSkipLocked(limite).stream().map(OutboxEventEntity::toDomain).toList();
    }
}