package com.fiap.techchallenge.execution.diagnostico.adapter.gateway;

import com.fiap.techchallenge.execution.diagnostico.domain.DiagnosticoItem;
import com.fiap.techchallenge.execution.diagnostico.domain.TipoItemDiagnostico;
import com.fiap.techchallenge.execution.support.PostgresContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(DiagnosticoGatewayImpl.class)
class DiagnosticoGatewayImplIT extends PostgresContainerSupport {

    @Autowired
    DiagnosticoGatewayImpl gateway;

    @Test
    void substituiOsItensAnterioresDaMesmaOs() {
        gateway.substituirItens(1L, List.of(DiagnosticoItem.criar(1L, TipoItemDiagnostico.PECA, 10L, 2)));
        gateway.substituirItens(1L, List.of(
                DiagnosticoItem.criar(1L, TipoItemDiagnostico.SERVICO, 5L, 1),
                DiagnosticoItem.criar(1L, TipoItemDiagnostico.PECA, 11L, 4)));

        assertThat(gateway.listarPorOsId(1L)).extracting(DiagnosticoItem::getRefId, DiagnosticoItem::getQuantidade)
                .containsExactly(tuple(5L, 1), tuple(11L, 4));
    }

    @Test
    void naoMexeNosItensDeOutraOs() {
        gateway.substituirItens(2L, List.of(DiagnosticoItem.criar(2L, TipoItemDiagnostico.PECA, 10L, 1)));
        gateway.substituirItens(3L, List.of(DiagnosticoItem.criar(3L, TipoItemDiagnostico.PECA, 10L, 1)));

        assertThat(gateway.listarPorOsId(2L)).hasSize(1);
        assertThat(gateway.listarPorOsId(3L)).hasSize(1);
        assertThat(gateway.listarPorOsId(99L)).isEmpty();
    }
}