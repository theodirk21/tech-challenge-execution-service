package com.fiap.techchallenge.execution.fila.adapter.controller.dto;

import com.fiap.techchallenge.execution.fila.domain.StatusFila;

import java.time.LocalDateTime;

public record ItemFilaResponse(Long osId, StatusFila status, int prioridade, Long mecanicoId, String solicitacao,
                               String veiculoPlaca, String veiculoModelo, LocalDateTime entrouEm,
                               LocalDateTime iniciouEm, LocalDateTime finalizouEm) {}