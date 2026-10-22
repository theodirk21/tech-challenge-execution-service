package com.fiap.techchallenge.execution.servico.usecase;

import com.fiap.techchallenge.execution.servico.domain.Servico;

import java.util.List;

public interface ServicoUseCase {

    List<Servico> listarPorOs(Long osId);

    void criarParaOs(Long osId, List<Long> tiposServicoIds);

    void iniciarDaOs(Long osId);

    void finalizarDaOs(Long osId);

    /** Compensação: descarta os serviços que ainda não começaram. */
    void cancelarPendentesDaOs(Long osId);
}