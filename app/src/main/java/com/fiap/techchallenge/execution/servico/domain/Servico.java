package com.fiap.techchallenge.execution.servico.domain;

import com.fiap.techchallenge.execution.exception.ServicoBadStatusException;

import java.time.LocalDateTime;

/**
 * Execução de um tipo de serviço dentro de uma OS. Referencia a OS e o tipo de serviço apenas por id,
 * pois esses dados pertencem ao os-service e ao billing-service.
 */
public class Servico {

    private Long id;
    private Long tipoServicoId;
    private Long osId;
    private ServicoStatus status;
    private LocalDateTime dataInicio;
    private LocalDateTime dataFim;

    public Servico() {}

    public Servico(Long id, Long tipoServicoId, Long osId, ServicoStatus status,
                   LocalDateTime dataInicio, LocalDateTime dataFim) {
        this.id = id;
        this.tipoServicoId = tipoServicoId;
        this.osId = osId;
        this.status = status;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
    }

    public static Servico criar(Long tipoServicoId, Long osId) {
        return new Servico(null, tipoServicoId, osId, ServicoStatus.AGUARDANDO_INICIO, null, null);
    }

    public Long getId() { return id; }
    public Long getTipoServicoId() { return tipoServicoId; }
    public Long getOsId() { return osId; }
    public ServicoStatus getStatus() { return status; }
    public LocalDateTime getDataInicio() { return dataInicio; }
    public LocalDateTime getDataFim() { return dataFim; }

    public void setId(Long id) { this.id = id; }
    public void setTipoServicoId(Long tipoServicoId) { this.tipoServicoId = tipoServicoId; }
    public void setOsId(Long osId) { this.osId = osId; }
    public void setStatus(ServicoStatus status) { this.status = status; }
    public void setDataInicio(LocalDateTime dataInicio) { this.dataInicio = dataInicio; }
    public void setDataFim(LocalDateTime dataFim) { this.dataFim = dataFim; }

    public void atualizar(Long novaOsId, Long novoTipoServicoId) {
        if (this.status == ServicoStatus.FINALIZADO) {
            throw new ServicoBadStatusException("Não pode atualizar finalizado");
        }
        if (novaOsId != null) {
            this.osId = novaOsId;
        }
        if (novoTipoServicoId != null) {
            this.tipoServicoId = novoTipoServicoId;
        }
    }

    public void iniciar() {
        if (status != ServicoStatus.AGUARDANDO_INICIO) {
            throw new ServicoBadStatusException("Serviço já iniciado ou finalizado");
        }
        this.status = ServicoStatus.EM_EXECUCAO;
        this.dataInicio = LocalDateTime.now();
    }

    public void finalizar() {
        if (status != ServicoStatus.EM_EXECUCAO) {
            throw new ServicoBadStatusException("Só pode finalizar em execução");
        }
        this.status = ServicoStatus.FINALIZADO;
        this.dataFim = LocalDateTime.now();
    }

    public void deletar() {
        if (status == ServicoStatus.EM_EXECUCAO) {
            throw new ServicoBadStatusException("Não pode deletar em execução");
        }
        this.status = ServicoStatus.DELETADO;
    }
}
