package com.fiap.techchallenge.execution.estoque.adapter.gateway;

import com.fiap.techchallenge.execution.estoque.domain.PecaEstoque;
import com.fiap.techchallenge.execution.estoque.domain.ReservaPeca;
import com.fiap.techchallenge.execution.estoque.domain.ReservaStatus;
import com.fiap.techchallenge.execution.support.PostgresContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(EstoqueGatewayImpl.class)
class EstoqueGatewayImplIT extends PostgresContainerSupport {

    @Autowired
    EstoqueGatewayImpl gateway;

    @Test
    void reservaEPersisteAQuantidadeRestanteDaPeca() {
        PecaEstoque peca = gateway.salvarPeca(new PecaEstoque(null, "Filtro de óleo", 10));
        PecaEstoque travada = gateway.buscarPecaPorIdComLock(peca.getId()).orElseThrow();
        travada.reservar(3);
        gateway.salvarPeca(travada);

        assertThat(gateway.buscarPecaPorId(peca.getId()).orElseThrow().getQuantidade()).isEqualTo(7);
    }

    @Test
    void buscaPecaInexistenteRetornaVazio() {
        assertThat(gateway.buscarPecaPorId(-1L)).isEmpty();
        assertThat(gateway.buscarPecaPorIdComLock(-1L)).isEmpty();
    }

    @Test
    void listaPecas() {
        gateway.salvarPeca(new PecaEstoque(null, "Pastilha", 4));

        assertThat(gateway.listarPecas()).extracting(PecaEstoque::getNome).contains("Pastilha");
    }

    @Test
    void persisteEAtualizaStatusDaReserva() {
        PecaEstoque peca = gateway.salvarPeca(new PecaEstoque(null, "Correia", 5));
        ReservaPeca reserva = gateway.salvarReserva(ReservaPeca.criar(50L, peca.getId(), 2));
        reserva.liberar();
        gateway.salvarReserva(reserva);

        assertThat(gateway.listarReservasPorOsId(50L)).singleElement().satisfies(r -> {
            assertThat(r.getStatus()).isEqualTo(ReservaStatus.LIBERADA);
            assertThat(r.getQuantidade()).isEqualTo(2);
            assertThat(r.getPecaId()).isEqualTo(peca.getId());
        });
    }
}