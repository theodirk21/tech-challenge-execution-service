package com.fiap.techchallenge.execution.estoque.usecase;

public record ResultadoReserva(boolean sucesso, String detalhe) {

    public static ResultadoReserva ok() {
        return new ResultadoReserva(true, null);
    }

    public static ResultadoReserva falha(String detalhe) {
        return new ResultadoReserva(false, detalhe);
    }
}