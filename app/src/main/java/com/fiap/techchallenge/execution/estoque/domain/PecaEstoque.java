package com.fiap.techchallenge.execution.estoque.domain;

import com.fiap.techchallenge.execution.exception.BadRequestException;

public class PecaEstoque {

    private Long id;
    private String nome;
    private Integer quantidade;

    public PecaEstoque() {}

    public PecaEstoque(Long id, String nome, Integer quantidade) {
        this.id = id;
        this.nome = nome;
        this.quantidade = quantidade;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public Integer getQuantidade() { return quantidade; }

    public void setId(Long id) { this.id = id; }
    public void setNome(String nome) { this.nome = nome; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }

    public boolean temEstoque(int qtd) {
        return quantidade >= qtd;
    }

    public void repor(int qtd) {
        if (qtd <= 0) {
            throw new BadRequestException("Erro: A quantidade de reposição deve ser maior que zero!");
        }
        this.quantidade += qtd;
    }

    public void reservar(int qtd) {
        if (quantidade < qtd) {
            throw new BadRequestException("Erro: A peça id '%d' não possui a quantidade necessária em estoque! Requisitado: '%d', disponível: '%d'"
                    .formatted(id, qtd, quantidade));
        }
        this.quantidade -= qtd;
    }

    public void liberar(int qtd) {
        this.quantidade += qtd;
    }
}
