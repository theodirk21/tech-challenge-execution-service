package com.fiap.techchallenge.execution.fila.adapter.controller.dto;

import com.fiap.techchallenge.execution.diagnostico.domain.TipoItemDiagnostico;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ItemDiagnosticoRequest(
        @NotNull(message = "Tipo do item não pode ser nulo!") TipoItemDiagnostico tipo,
        @NotNull(message = "Referência do item não pode ser nula!") Long refId,
        @NotNull(message = "Quantidade não pode ser nula!") @Min(value = 1, message = "Quantidade deve ser maior que zero!") Integer quantidade) {}