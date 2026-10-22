package com.fiap.techchallenge.execution.fila.adapter.controller.dto;

import com.fiap.techchallenge.execution.diagnostico.domain.TipoItemDiagnostico;

public record DiagnosticoItemResponse(TipoItemDiagnostico tipo, Long refId, int quantidade) {}