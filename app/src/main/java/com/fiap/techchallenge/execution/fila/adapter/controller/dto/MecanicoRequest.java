package com.fiap.techchallenge.execution.fila.adapter.controller.dto;

import jakarta.validation.constraints.NotNull;

public record MecanicoRequest(@NotNull(message = "Id do mecânico não pode ser nulo!") Long mecanicoId) {}