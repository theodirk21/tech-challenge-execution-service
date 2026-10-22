package com.fiap.techchallenge.execution.diagnostico.framework.persistence.entity;

import com.fiap.techchallenge.execution.diagnostico.domain.DiagnosticoItem;
import com.fiap.techchallenge.execution.diagnostico.domain.TipoItemDiagnostico;
import jakarta.persistence.*;

@Entity
@Table(name = "diagnostico_item")
public class DiagnosticoItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "os_id", nullable = false)
    private Long osId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoItemDiagnostico tipo;

    @Column(name = "ref_id", nullable = false)
    private Long refId;

    @Column(nullable = false)
    private int quantidade;

    protected DiagnosticoItemEntity() {}

    public static DiagnosticoItemEntity from(DiagnosticoItem d) {
        DiagnosticoItemEntity e = new DiagnosticoItemEntity();
        e.id = d.getId();
        e.osId = d.getOsId();
        e.tipo = d.getTipo();
        e.refId = d.getRefId();
        e.quantidade = d.getQuantidade();
        return e;
    }

    public DiagnosticoItem toDomain() {
        return new DiagnosticoItem(id, osId, tipo, refId, quantidade);
    }
}