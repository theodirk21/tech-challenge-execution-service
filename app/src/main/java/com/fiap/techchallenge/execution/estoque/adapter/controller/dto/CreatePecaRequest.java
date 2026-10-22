package com.fiap.techchallenge.execution.estoque.adapter.controller.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreatePecaRequest(
        @NotBlank(message = "Nome da peça não pode ser vazio!") String nome,
        @NotNull(message = "Quantidade não pode ser nula!") @Min(value = 0, message = "Quantidade não pode ser negativa!") Integer quantidade) {}