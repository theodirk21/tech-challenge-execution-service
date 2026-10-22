package com.fiap.techchallenge.execution.estoque.adapter.controller;

import com.fiap.techchallenge.execution.estoque.adapter.controller.dto.PecaResponse;
import com.fiap.techchallenge.execution.estoque.domain.PecaEstoque;
import org.springframework.stereotype.Component;

@Component
public class PecaMapper {

    public PecaResponse toResponse(PecaEstoque peca) {
        return new PecaResponse(peca.getId(), peca.getNome(), peca.getQuantidade());
    }
}