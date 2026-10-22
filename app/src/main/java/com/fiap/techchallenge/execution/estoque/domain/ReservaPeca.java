package com.fiap.techchallenge.execution.estoque.domain;

import com.fiap.techchallenge.execution.exception.ConflictException;

public class ReservaPeca {

    private Long id;
    private Long osId;
    private Long pecaId;
    private int quantidade;
    private ReservaStatus status;

    public ReservaPeca() {}

    public ReservaPeca(Long id, Long osId, Long pecaId, int quantidade, ReservaStatus status) {
        this.id = id;
        this.osId = osId;
        this.pecaId = pecaId;
        this.quantidade = quantidade;
        this.status = status;
    }

    public static ReservaPeca criar(Long osId, Long pecaId, int quantidade) {
        return new ReservaPeca(null, osId, pecaId, quantidade, ReservaStatus.RESERVADA);
    }

    /** Compensação: devolve a reserva ao estoque (a quantidade é devolvida pelo PecaEstoque). */
    public void liberar() {
        exigirReservada("liberar");
        this.status = ReservaStatus.LIBERADA;
    }

    /** Consome a reserva quando a execução começa. */
    public void baixar() {
        exigirReservada("baixar");
        this.status = ReservaStatus.BAIXADA;
    }

    public boolean estaReservada() {
        return status == ReservaStatus.RESERVADA;
    }

    private void exigirReservada(String acao) {
        if (status != ReservaStatus.RESERVADA) {
            throw new ConflictException("Erro: Não é possível %s a reserva da peça %d da OS %d; ela está '%s'!"
                    .formatted(acao, pecaId, osId, status));
        }
    }

    public Long getId() { return id; }
    public Long getOsId() { return osId; }
    public Long getPecaId() { return pecaId; }
    public int getQuantidade() { return quantidade; }
    public ReservaStatus getStatus() { return status; }

    public void setId(Long id) { this.id = id; }
}