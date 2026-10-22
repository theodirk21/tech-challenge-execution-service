package com.fiap.techchallenge.execution.mensageria.domain;

public final class TipoEvento {

    public static final String DIAGNOSTICO_CONCLUIDO = "DIAGNOSTICO_CONCLUIDO";
    public static final String ESTOQUE_RESERVADO = "ESTOQUE_RESERVADO";
    public static final String EXECUCAO_INICIADA = "EXECUCAO_INICIADA";
    public static final String EXECUCAO_FINALIZADA = "EXECUCAO_FINALIZADA";
    public static final String EXECUCAO_FALHOU = "EXECUCAO_FALHOU";

    private TipoEvento() {}
}