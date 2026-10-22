package com.fiap.techchallenge.execution.estoque.adapter.controller;

import com.fiap.techchallenge.execution.estoque.adapter.controller.dto.CreatePecaRequest;
import com.fiap.techchallenge.execution.estoque.adapter.controller.dto.PecaResponse;
import com.fiap.techchallenge.execution.estoque.adapter.controller.dto.ReporEstoqueRequest;
import com.fiap.techchallenge.execution.estoque.usecase.EstoqueUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping({"/api/peca", "/api/peca/"})
@Tag(name = "Peças", description = "Estoque de peças do serviço de execução")
public class PecaController {

    private final EstoqueUseCase estoque;
    private final PecaMapper mapper;

    public PecaController(EstoqueUseCase estoque, PecaMapper mapper) {
        this.estoque = estoque;
        this.mapper = mapper;
    }

    @GetMapping
    @Operation(summary = "Listar peças em estoque")
    public ResponseEntity<List<PecaResponse>> listar() {
        return ResponseEntity.ok(estoque.listarPecas().stream().map(mapper::toResponse).toList());
    }

    @GetMapping("{id}")
    @Operation(summary = "Buscar peça por ID")
    public ResponseEntity<PecaResponse> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(mapper.toResponse(estoque.buscarPeca(id)));
    }

    @PostMapping
    @Operation(summary = "Cadastrar peça no estoque")
    public ResponseEntity<PecaResponse> criar(@Valid @RequestBody CreatePecaRequest request) {
        var peca = estoque.criarPeca(request.nome(), request.quantidade());
        return ResponseEntity.created(URI.create(peca.getId().toString())).body(mapper.toResponse(peca));
    }

    @PostMapping("{id}/reposicao")
    @Operation(summary = "Repor estoque", description = "Soma a quantidade informada ao estoque da peça")
    public ResponseEntity<PecaResponse> repor(@PathVariable Long id, @Valid @RequestBody ReporEstoqueRequest request) {
        return ResponseEntity.ok(mapper.toResponse(estoque.reporEstoque(id, request.quantidade())));
    }
}