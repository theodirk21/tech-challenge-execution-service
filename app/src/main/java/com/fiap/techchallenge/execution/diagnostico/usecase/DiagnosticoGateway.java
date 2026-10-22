package com.fiap.techchallenge.execution.diagnostico.usecase;

import com.fiap.techchallenge.execution.diagnostico.domain.DiagnosticoItem;

import java.util.List;

public interface DiagnosticoGateway {

    List<DiagnosticoItem> substituirItens(Long osId, List<DiagnosticoItem> itens);

    List<DiagnosticoItem> listarPorOsId(Long osId);
}