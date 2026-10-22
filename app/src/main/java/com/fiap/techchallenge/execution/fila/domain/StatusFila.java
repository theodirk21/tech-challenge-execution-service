package com.fiap.techchallenge.execution.fila.domain;

public enum StatusFila {
    AGUARDANDO_DIAGNOSTICO,
    EM_DIAGNOSTICO,
    AGUARDANDO_PAGAMENTO,
    NA_FILA,
    EM_EXECUCAO,
    FINALIZADA,
    FALHOU,
    CANCELADA;

    public boolean isTerminal() {
        return this == FINALIZADA || this == FALHOU || this == CANCELADA;
    }
}
