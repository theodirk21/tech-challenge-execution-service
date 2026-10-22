package com.fiap.techchallenge.execution.fila.adapter.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record FalhaExecucaoRequest(
        @NotBlank(message = "Motivo não pode ser vazio!") String motivo,
        String detalhe) {}