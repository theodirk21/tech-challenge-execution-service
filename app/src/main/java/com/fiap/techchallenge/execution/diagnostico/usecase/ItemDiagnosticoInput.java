package com.fiap.techchallenge.execution.diagnostico.usecase;

import com.fiap.techchallenge.execution.diagnostico.domain.TipoItemDiagnostico;

public record ItemDiagnosticoInput(TipoItemDiagnostico tipo, Long refId, int quantidade) {}