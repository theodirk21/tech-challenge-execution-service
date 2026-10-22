package com.fiap.techchallenge.execution.estoque.adapter.controller.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReporEstoqueRequest(
        @NotNull(message = "Quantidade não pode ser nula!") @Min(value = 1, message = "Quantidade deve ser maior que zero!") Integer quantidade) {}