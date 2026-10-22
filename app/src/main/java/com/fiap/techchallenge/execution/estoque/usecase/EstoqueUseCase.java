package com.fiap.techchallenge.execution.estoque.usecase;

import com.fiap.techchallenge.execution.estoque.domain.PecaEstoque;

import java.util.List;

public interface EstoqueUseCase {

    List<PecaEstoque> listarPecas();

    PecaEstoque buscarPeca(Long id);

    PecaEstoque criarPeca(String nome, int quantidade);

    PecaEstoque reporEstoque(Long id, int quantidade);

    boolean existePeca(Long id);

    /** Reserva todas as peças ou nenhuma: se faltar estoque, nada é alterado e o resultado traz o detalhe. */
    ResultadoReserva reservarPecas(Long osId, List<ItemReserva> itens);

    /** Compensação: devolve ao estoque as reservas ainda ativas da OS. */
    void liberarReservas(Long osId);

    /** Consome as reservas ativas da OS quando o serviço termina. */
    void baixarReservas(Long osId);
}