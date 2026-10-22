package com.fiap.techchallenge.execution.fila.adapter.controller;

import com.fiap.techchallenge.execution.diagnostico.domain.DiagnosticoItem;
import com.fiap.techchallenge.execution.diagnostico.usecase.ItemDiagnosticoInput;
import com.fiap.techchallenge.execution.fila.adapter.controller.dto.DiagnosticoItemResponse;
import com.fiap.techchallenge.execution.fila.adapter.controller.dto.ItemDiagnosticoRequest;
import com.fiap.techchallenge.execution.fila.adapter.controller.dto.ItemFilaResponse;
import com.fiap.techchallenge.execution.fila.adapter.controller.dto.ServicoExecucaoResponse;
import com.fiap.techchallenge.execution.fila.domain.ItemFila;
import com.fiap.techchallenge.execution.servico.domain.Servico;
import org.springframework.stereotype.Component;

@Component
public class ExecucaoMapper {

    public ItemFilaResponse toResponse(ItemFila i) {
        return new ItemFilaResponse(i.getOsId(), i.getStatus(), i.getPrioridade(), i.getMecanicoId(),
                i.getSolicitacao(), i.getVeiculoPlaca(), i.getVeiculoModelo(), i.getEntrouEm(),
                i.getIniciouEm(), i.getFinalizouEm());
    }

    public DiagnosticoItemResponse toResponse(DiagnosticoItem d) {
        return new DiagnosticoItemResponse(d.getTipo(), d.getRefId(), d.getQuantidade());
    }

    public ServicoExecucaoResponse toResponse(Servico s) {
        return new ServicoExecucaoResponse(s.getId(), s.getTipoServicoId(), s.getStatus(), s.getDataInicio(), s.getDataFim());
    }

    public ItemDiagnosticoInput toInput(ItemDiagnosticoRequest r) {
        return new ItemDiagnosticoInput(r.tipo(), r.refId(), r.quantidade());
    }
}