package com.fiap.techchallenge.execution.mensageria.adapter.gateway;

import com.fiap.techchallenge.execution.mensageria.framework.persistence.repository.ProcessedEventRepository;
import com.fiap.techchallenge.execution.mensageria.usecase.ProcessedEventGateway;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public class ProcessedEventGatewayImpl implements ProcessedEventGateway {

    private final ProcessedEventRepository repository;

    public ProcessedEventGatewayImpl(ProcessedEventRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean registrar(UUID eventId, LocalDateTime agora) {
        return repository.inserirSeAusente(eventId, agora) == 1;
    }
}