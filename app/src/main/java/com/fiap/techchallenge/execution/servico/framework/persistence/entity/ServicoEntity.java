package com.fiap.techchallenge.execution.servico.framework.persistence.entity;

import com.fiap.techchallenge.execution.servico.domain.Servico;
import com.fiap.techchallenge.execution.servico.domain.ServicoStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "servico_execucao")
public class ServicoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "os_id", nullable = false)
    private Long osId;

    @Column(name = "tipo_servico_id", nullable = false)
    private Long tipoServicoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ServicoStatus status;

    @Column(name = "data_inicio")
    private LocalDateTime dataInicio;

    @Column(name = "data_fim")
    private LocalDateTime dataFim;

    protected ServicoEntity() {}

    public static ServicoEntity from(Servico d) {
        ServicoEntity e = new ServicoEntity();
        e.id = d.getId();
        e.osId = d.getOsId();
        e.tipoServicoId = d.getTipoServicoId();
        e.status = d.getStatus();
        e.dataInicio = d.getDataInicio();
        e.dataFim = d.getDataFim();
        return e;
    }

    public Servico toDomain() {
        return new Servico(id, tipoServicoId, osId, status, dataInicio, dataFim);
    }
}