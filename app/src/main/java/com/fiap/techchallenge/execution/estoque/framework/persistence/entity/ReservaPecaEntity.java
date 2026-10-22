package com.fiap.techchallenge.execution.estoque.framework.persistence.entity;

import com.fiap.techchallenge.execution.estoque.domain.ReservaPeca;
import com.fiap.techchallenge.execution.estoque.domain.ReservaStatus;
import jakarta.persistence.*;

@Entity
@Table(name = "reserva_peca")
public class ReservaPecaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "os_id", nullable = false)
    private Long osId;

    @Column(name = "peca_id", nullable = false)
    private Long pecaId;

    @Column(nullable = false)
    private int quantidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private ReservaStatus status;

    protected ReservaPecaEntity() {}

    public static ReservaPecaEntity from(ReservaPeca d) {
        ReservaPecaEntity e = new ReservaPecaEntity();
        e.id = d.getId();
        e.osId = d.getOsId();
        e.pecaId = d.getPecaId();
        e.quantidade = d.getQuantidade();
        e.status = d.getStatus();
        return e;
    }

    public ReservaPeca toDomain() {
        return new ReservaPeca(id, osId, pecaId, quantidade, status);
    }
}