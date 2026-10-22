package com.fiap.techchallenge.execution.fila.framework.persistence.entity;

import com.fiap.techchallenge.execution.fila.domain.ItemFila;
import com.fiap.techchallenge.execution.fila.domain.StatusFila;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "fila_execucao")
public class ItemFilaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "os_id", nullable = false, unique = true)
    private Long osId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusFila status;

    @Column(nullable = false)
    private int prioridade;

    @Column(name = "mecanico_id")
    private Long mecanicoId;

    private String solicitacao;

    @Column(name = "veiculo_placa", length = 10)
    private String veiculoPlaca;

    @Column(name = "veiculo_modelo", length = 80)
    private String veiculoModelo;

    @Column(name = "entrou_em", nullable = false)
    private LocalDateTime entrouEm;

    @Column(name = "iniciou_em")
    private LocalDateTime iniciouEm;

    @Column(name = "finalizou_em")
    private LocalDateTime finalizouEm;

    protected ItemFilaEntity() {}

    public static ItemFilaEntity from(ItemFila d) {
        ItemFilaEntity e = new ItemFilaEntity();
        e.id = d.getId();
        e.osId = d.getOsId();
        e.status = d.getStatus();
        e.prioridade = d.getPrioridade();
        e.mecanicoId = d.getMecanicoId();
        e.solicitacao = d.getSolicitacao();
        e.veiculoPlaca = d.getVeiculoPlaca();
        e.veiculoModelo = d.getVeiculoModelo();
        e.entrouEm = d.getEntrouEm();
        e.iniciouEm = d.getIniciouEm();
        e.finalizouEm = d.getFinalizouEm();
        return e;
    }

    public ItemFila toDomain() {
        return new ItemFila(id, osId, status, prioridade, mecanicoId, solicitacao,
                veiculoPlaca, veiculoModelo, entrouEm, iniciouEm, finalizouEm);
    }
}
