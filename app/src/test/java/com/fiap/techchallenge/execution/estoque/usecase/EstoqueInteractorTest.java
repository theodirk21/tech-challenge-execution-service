package com.fiap.techchallenge.execution.estoque.usecase;

import com.fiap.techchallenge.execution.estoque.domain.PecaEstoque;
import com.fiap.techchallenge.execution.estoque.domain.ReservaPeca;
import com.fiap.techchallenge.execution.estoque.domain.ReservaStatus;
import com.fiap.techchallenge.execution.exception.BadRequestException;
import com.fiap.techchallenge.execution.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EstoqueInteractorTest {

    @Mock
    EstoqueGateway gateway;

    EstoqueInteractor interactor;

    @BeforeEach
    void setUp() {
        interactor = new EstoqueInteractor(gateway);
        when(gateway.salvarPeca(any())).thenAnswer(i -> i.getArgument(0));
        when(gateway.salvarReserva(any())).thenAnswer(i -> i.getArgument(0));
    }

    private void peca(long id, int qtd) {
        when(gateway.buscarPecaPorIdComLock(id)).thenReturn(Optional.of(new PecaEstoque(id, "P" + id, qtd)));
    }

    @Test
    void reservaTodasAsPecasSomandoRepetidas() {
        peca(7L, 10);
        peca(8L, 5);

        ResultadoReserva r = interactor.reservarPecas(1L, List.of(
                new ItemReserva(7L, 2), new ItemReserva(8L, 1), new ItemReserva(7L, 3)));

        assertThat(r.sucesso()).isTrue();
        ArgumentCaptor<ReservaPeca> reservas = ArgumentCaptor.forClass(ReservaPeca.class);
        verify(gateway, org.mockito.Mockito.times(2)).salvarReserva(reservas.capture());
        assertThat(reservas.getAllValues()).extracting(ReservaPeca::getPecaId, ReservaPeca::getQuantidade)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(7L, 5), org.assertj.core.groups.Tuple.tuple(8L, 1));
    }

    @Test
    void semEstoqueNaoAlteraNada() {
        peca(7L, 10);
        peca(8L, 0);

        ResultadoReserva r = interactor.reservarPecas(1L, List.of(new ItemReserva(7L, 2), new ItemReserva(8L, 2)));

        assertThat(r.sucesso()).isFalse();
        assertThat(r.detalhe()).isEqualTo("Peça 8: requisitado 2, disponível 0");
        verify(gateway, never()).salvarPeca(any());
        verify(gateway, never()).salvarReserva(any());
    }

    @Test
    void pecaInexistenteFalhaNaReserva() {
        when(gateway.buscarPecaPorIdComLock(9L)).thenReturn(Optional.empty());

        ResultadoReserva r = interactor.reservarPecas(1L, List.of(new ItemReserva(9L, 1)));

        assertThat(r.sucesso()).isFalse();
        assertThat(r.detalhe()).contains("não encontrada");
    }

    @Test
    void liberaApenasReservasAtivasDevolvendoAoEstoque() {
        peca(7L, 5);
        ReservaPeca ativa = ReservaPeca.criar(1L, 7L, 3);
        ReservaPeca ja = ReservaPeca.criar(1L, 7L, 9);
        ja.baixar();
        when(gateway.listarReservasPorOsId(1L)).thenReturn(List.of(ativa, ja));

        interactor.liberarReservas(1L);

        assertThat(ativa.getStatus()).isEqualTo(ReservaStatus.LIBERADA);
        assertThat(ja.getStatus()).isEqualTo(ReservaStatus.BAIXADA);
        ArgumentCaptor<PecaEstoque> peca = ArgumentCaptor.forClass(PecaEstoque.class);
        verify(gateway).salvarPeca(peca.capture());
        assertThat(peca.getValue().getQuantidade()).isEqualTo(8);
    }

    @Test
    void baixaReservasAtivas() {
        ReservaPeca ativa = ReservaPeca.criar(1L, 7L, 3);
        when(gateway.listarReservasPorOsId(1L)).thenReturn(List.of(ativa));

        interactor.baixarReservas(1L);

        assertThat(ativa.getStatus()).isEqualTo(ReservaStatus.BAIXADA);
    }

    @Test
    void criaPecaValidandoEntrada() {
        assertThat(interactor.criarPeca("Filtro", 3).getQuantidade()).isEqualTo(3);
        assertThatThrownBy(() -> interactor.criarPeca(" ", 3)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> interactor.criarPeca("X", -1)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void reporEstoqueSomaQuantidade() {
        peca(7L, 5);

        assertThat(interactor.reporEstoque(7L, 4).getQuantidade()).isEqualTo(9);
        assertThatThrownBy(() -> interactor.reporEstoque(7L, 0)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void consultasEPecaInexistente() {
        when(gateway.buscarPecaPorId(1L)).thenReturn(Optional.of(new PecaEstoque(1L, "A", 1)));
        when(gateway.buscarPecaPorId(2L)).thenReturn(Optional.empty());
        when(gateway.listarPecas()).thenReturn(List.of(new PecaEstoque(1L, "A", 1)));
        when(gateway.buscarPecaPorIdComLock(2L)).thenReturn(Optional.empty());

        assertThat(interactor.existePeca(1L)).isTrue();
        assertThat(interactor.existePeca(2L)).isFalse();
        assertThat(interactor.listarPecas()).hasSize(1);
        assertThat(interactor.buscarPeca(1L).getNome()).isEqualTo("A");
        assertThatThrownBy(() -> interactor.buscarPeca(2L)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> interactor.reporEstoque(2L, 1)).isInstanceOf(NotFoundException.class);
    }
}