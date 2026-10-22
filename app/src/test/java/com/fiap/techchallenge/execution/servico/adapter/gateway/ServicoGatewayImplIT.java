package com.fiap.techchallenge.execution.servico.adapter.gateway;

import com.fiap.techchallenge.execution.servico.domain.Servico;
import com.fiap.techchallenge.execution.servico.domain.ServicoStatus;
import com.fiap.techchallenge.execution.support.PostgresContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ServicoGatewayImpl.class)
class ServicoGatewayImplIT extends PostgresContainerSupport {

    @Autowired
    ServicoGatewayImpl gateway;

    @Test
    void salvaEBuscaPorId() {
        Servico salvo = gateway.salvar(Servico.criar(7L, 300L));

        Servico lido = gateway.buscarPorId(salvo.getId()).orElseThrow();
        assertThat(lido.getOsId()).isEqualTo(300L);
        assertThat(lido.getTipoServicoId()).isEqualTo(7L);
        assertThat(lido.getStatus()).isEqualTo(ServicoStatus.AGUARDANDO_INICIO);
    }

    @Test
    void persisteInicioEFim() {
        Servico s = gateway.salvar(Servico.criar(1L, 301L));
        s.iniciar();
        s.finalizar();
        gateway.salvar(s);

        Servico lido = gateway.listarPorOsId(301L).get(0);
        assertThat(lido.getStatus()).isEqualTo(ServicoStatus.FINALIZADO);
        assertThat(lido.getDataInicio()).isNotNull();
        assertThat(lido.getDataFim()).isNotNull();
    }

    @Test
    void buscaInexistenteRetornaVazio() {
        assertThat(gateway.buscarPorId(-1L)).isEmpty();
    }
}