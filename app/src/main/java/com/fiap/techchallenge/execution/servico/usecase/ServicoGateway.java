package com.fiap.techchallenge.execution.servico.usecase;

import com.fiap.techchallenge.execution.servico.domain.Servico;

import java.util.List;
import java.util.Optional;

public interface ServicoGateway {

    Servico salvar(Servico servico);

    Optional<Servico> buscarPorId(Long id);

    List<Servico> listarPorOsId(Long osId);
}