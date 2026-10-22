package com.fiap.techchallenge.execution.estoque.framework.persistence.entity;

import com.fiap.techchallenge.execution.estoque.domain.PecaEstoque;
import jakarta.persistence.*;

@Entity
@Table(name = "peca_estoque")
public class PecaEstoqueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false)
    private Integer quantidade;

    protected PecaEstoqueEntity() {}

    public static PecaEstoqueEntity from(PecaEstoque d) {
        PecaEstoqueEntity e = new PecaEstoqueEntity();
        e.id = d.getId();
        e.nome = d.getNome();
        e.quantidade = d.getQuantidade();
        return e;
    }

    public PecaEstoque toDomain() {
        return new PecaEstoque(id, nome, quantidade);
    }
}