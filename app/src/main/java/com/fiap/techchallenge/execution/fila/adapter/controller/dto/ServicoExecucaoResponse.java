package com.fiap.techchallenge.execution.fila.adapter.controller.dto;

import com.fiap.techchallenge.execution.servico.domain.ServicoStatus;

import java.time.LocalDateTime;

public record ServicoExecucaoResponse(Long id, Long tipoServicoId, ServicoStatus status,
                                      LocalDateTime dataInicio, LocalDateTime dataFim) {}