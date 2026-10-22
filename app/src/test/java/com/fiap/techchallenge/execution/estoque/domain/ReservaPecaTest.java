package com.fiap.techchallenge.execution.estoque.domain;

import com.fiap.techchallenge.execution.exception.BadRequestException;
import com.fiap.techchallenge.execution.exception.ConflictException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservaPecaTest {

    @Test
    void nasceReservada() {
        assertThat(ReservaPeca.criar(1L, 2L, 3).estaReservada()).isTrue();
    }

    @Test
    void liberarEBaixarMudamStatus() {
        ReservaPeca a = ReservaPeca.criar(1L, 2L, 3);
        a.liberar();
        ReservaPeca b = ReservaPeca.criar(1L, 2L, 3);
        b.baixar();

        assertThat(a.getStatus()).isEqualTo(ReservaStatus.LIBERADA);
        assertThat(b.getStatus()).isEqualTo(ReservaStatus.BAIXADA);
    }

    @Test
    void naoPermiteLiberarOuBaixarDuasVezes() {
        ReservaPeca r = ReservaPeca.criar(1L, 2L, 3);
        r.liberar();

        assertThatThrownBy(r::liberar).isInstanceOf(ConflictException.class);
        assertThatThrownBy(r::baixar).isInstanceOf(ConflictException.class);
    }

    @Test
    void pecaSemEstoqueRecusaReserva() {
        PecaEstoque p = new PecaEstoque(1L, "Filtro", 1);

        assertThatThrownBy(() -> p.reservar(2)).isInstanceOf(BadRequestException.class);
        p.reservar(1);
        p.liberar(1);
        assertThat(p.getQuantidade()).isEqualTo(1);
    }
}