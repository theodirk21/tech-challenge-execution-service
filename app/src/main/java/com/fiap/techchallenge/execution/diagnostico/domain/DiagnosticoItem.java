package com.fiap.techchallenge.execution.diagnostico.domain;

import com.fiap.techchallenge.execution.exception.BadRequestException;

/** Item (peça ou serviço) apontado no diagnóstico de uma OS; refId aponta para o catálogo de outro serviço. */
public class DiagnosticoItem {

    private Long id;
    private Long osId;
    private TipoItemDiagnostico tipo;
    private Long refId;
    private int quantidade;

    public DiagnosticoItem() {}

    public DiagnosticoItem(Long id, Long osId, TipoItemDiagnostico tipo, Long refId, int quantidade) {
        this.id = id;
        this.osId = osId;
        this.tipo = tipo;
        this.refId = refId;
        this.quantidade = quantidade;
    }

    public static DiagnosticoItem criar(Long osId, TipoItemDiagnostico tipo, Long refId, int quantidade) {
        if (quantidade <= 0) {
            throw new BadRequestException("Erro: A quantidade do item de diagnóstico deve ser maior que zero!");
        }
        return new DiagnosticoItem(null, osId, tipo, refId, quantidade);
    }

    public Long getId() { return id; }
    public Long getOsId() { return osId; }
    public TipoItemDiagnostico getTipo() { return tipo; }
    public Long getRefId() { return refId; }
    public int getQuantidade() { return quantidade; }

    public void setId(Long id) { this.id = id; }
}