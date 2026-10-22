package com.fiap.techchallenge.execution.diagnostico.domain;

import com.fiap.techchallenge.execution.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiagnosticoItemTest {

    @Test
    void criaItemValido() {
        DiagnosticoItem i = DiagnosticoItem.criar(1L, TipoItemDiagnostico.PECA, 9L, 2);

        assertThat(i.getQuantidade()).isEqualTo(2);
        assertThat(i.getTipo()).isEqualTo(TipoItemDiagnostico.PECA);
    }

    @Test
    void recusaQuantidadeNaoPositiva() {
        assertThatThrownBy(() -> DiagnosticoItem.criar(1L, TipoItemDiagnostico.PECA, 9L, 0))
                .isInstanceOf(BadRequestException.class);
    }
}