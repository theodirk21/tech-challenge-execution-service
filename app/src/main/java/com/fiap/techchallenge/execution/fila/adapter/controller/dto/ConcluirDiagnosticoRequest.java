package com.fiap.techchallenge.execution.fila.adapter.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ConcluirDiagnosticoRequest(
        @NotEmpty(message = "O diagnóstico deve ter ao menos um item!") List<@Valid ItemDiagnosticoRequest> itens) {}