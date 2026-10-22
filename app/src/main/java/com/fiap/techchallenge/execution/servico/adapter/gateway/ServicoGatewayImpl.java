package com.fiap.techchallenge.execution.servico.adapter.gateway;

import com.fiap.techchallenge.execution.servico.domain.Servico;
import com.fiap.techchallenge.execution.servico.framework.persistence.entity.ServicoEntity;
import com.fiap.techchallenge.execution.servico.framework.persistence.repository.ServicoRepository;
import com.fiap.techchallenge.execution.servico.usecase.ServicoGateway;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ServicoGatewayImpl implements ServicoGateway {

    private final ServicoRepository repository;

    public ServicoGatewayImpl(ServicoRepository repository) {
        this.repository = repository;
    }

    @Override
    public Servico salvar(Servico servico) {
        Servico salvo = repository.save(ServicoEntity.from(servico)).toDomain();
        servico.setId(salvo.getId());
        return salvo;
    }

    @Override
    public Optional<Servico> buscarPorId(Long id) {
        return repository.findById(id).map(ServicoEntity::toDomain);
    }

    @Override
    public List<Servico> listarPorOsId(Long osId) {
        return repository.findByOsIdOrderById(osId).stream().map(ServicoEntity::toDomain).toList();
    }
}