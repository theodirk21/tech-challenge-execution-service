package com.fiap.techchallenge.execution.fila.adapter.controller;

import com.fiap.techchallenge.execution.fila.adapter.controller.dto.ConcluirDiagnosticoRequest;
import com.fiap.techchallenge.execution.fila.adapter.controller.dto.DiagnosticoItemResponse;
import com.fiap.techchallenge.execution.fila.adapter.controller.dto.FalhaExecucaoRequest;
import com.fiap.techchallenge.execution.fila.adapter.controller.dto.ItemFilaResponse;
import com.fiap.techchallenge.execution.fila.adapter.controller.dto.MecanicoRequest;
import com.fiap.techchallenge.execution.fila.adapter.controller.dto.ServicoExecucaoResponse;
import com.fiap.techchallenge.execution.fila.domain.StatusFila;
import com.fiap.techchallenge.execution.fila.usecase.FilaUseCase;
import com.fiap.techchallenge.execution.servico.usecase.ServicoUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/execucao", "/api/execucao/"})
@Tag(name = "Execução", description = "Fila de execução, diagnóstico e execução dos serviços da OS")
public class ExecucaoController {

    private final FilaUseCase fila;
    private final ServicoUseCase servicos;
    private final ExecucaoMapper mapper;

    public ExecucaoController(FilaUseCase fila, ServicoUseCase servicos, ExecucaoMapper mapper) {
        this.fila = fila;
        this.servicos = servicos;
        this.mapper = mapper;
    }

    @GetMapping("fila")
    @Operation(summary = "Listar a fila por status", description = "Por padrão lista os itens NA_FILA, por prioridade e antiguidade")
    public ResponseEntity<List<ItemFilaResponse>> listarFila(@RequestParam(defaultValue = "NA_FILA") StatusFila status) {
        return ResponseEntity.ok(fila.listarPorStatus(status).stream().map(mapper::toResponse).toList());
    }

    @GetMapping("{osId}")
    @Operation(summary = "Buscar o item da fila de uma OS")
    public ResponseEntity<ItemFilaResponse> buscar(@PathVariable Long osId) {
        return ResponseEntity.ok(mapper.toResponse(fila.buscarPorOsId(osId)));
    }

    @GetMapping("{osId}/diagnostico")
    @Operation(summary = "Listar os itens do diagnóstico da OS")
    public ResponseEntity<List<DiagnosticoItemResponse>> diagnostico(@PathVariable Long osId) {
        return ResponseEntity.ok(fila.listarDiagnostico(osId).stream().map(mapper::toResponse).toList());
    }

    @GetMapping("{osId}/servicos")
    @Operation(summary = "Listar os serviços em execução da OS")
    public ResponseEntity<List<ServicoExecucaoResponse>> servicos(@PathVariable Long osId) {
        return ResponseEntity.ok(servicos.listarPorOs(osId).stream().map(mapper::toResponse).toList());
    }

    @PostMapping("{osId}/diagnostico/iniciar")
    @Operation(summary = "Iniciar o diagnóstico", description = "AGUARDANDO_DIAGNOSTICO -> EM_DIAGNOSTICO")
    public ResponseEntity<ItemFilaResponse> iniciarDiagnostico(@PathVariable Long osId,
                                                               @Valid @RequestBody MecanicoRequest request) {
        return ResponseEntity.ok(mapper.toResponse(fila.iniciarDiagnostico(osId, request.mecanicoId())));
    }

    @PostMapping("{osId}/diagnostico/concluir")
    @Operation(summary = "Concluir o diagnóstico", description = "EM_DIAGNOSTICO -> AGUARDANDO_PAGAMENTO; publica DIAGNOSTICO_CONCLUIDO")
    public ResponseEntity<ItemFilaResponse> concluirDiagnostico(@PathVariable Long osId,
                                                                @Valid @RequestBody ConcluirDiagnosticoRequest request) {
        var itens = request.itens().stream().map(mapper::toInput).toList();
        return ResponseEntity.ok(mapper.toResponse(fila.concluirDiagnostico(osId, itens)));
    }

    @PostMapping("iniciar")
    @Operation(summary = "Iniciar a próxima execução da fila", description = "NA_FILA -> EM_EXECUCAO para o item de maior prioridade e mais antigo")
    public ResponseEntity<ItemFilaResponse> iniciarProxima(@Valid @RequestBody MecanicoRequest request) {
        return ResponseEntity.ok(mapper.toResponse(fila.iniciarProximaExecucao(request.mecanicoId())));
    }

    @PostMapping("{osId}/finalizar")
    @Operation(summary = "Finalizar a execução", description = "EM_EXECUCAO -> FINALIZADA; baixa as reservas e publica EXECUCAO_FINALIZADA")
    public ResponseEntity<ItemFilaResponse> finalizar(@PathVariable Long osId) {
        return ResponseEntity.ok(mapper.toResponse(fila.finalizarExecucao(osId)));
    }

    @PostMapping("{osId}/falha")
    @Operation(summary = "Registrar falha na execução", description = "Compensa liberando as peças e publica EXECUCAO_FALHOU")
    public ResponseEntity<ItemFilaResponse> falhar(@PathVariable Long osId,
                                                   @Valid @RequestBody FalhaExecucaoRequest request) {
        return ResponseEntity.ok(mapper.toResponse(fila.falharExecucao(osId, request.motivo(), request.detalhe())));
    }
}