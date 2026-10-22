package com.fiap.techchallenge.execution.mensageria.adapter.gateway;

import com.fiap.techchallenge.execution.mensageria.domain.OutboxEvent;
import com.fiap.techchallenge.execution.support.PostgresContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({OutboxGatewayImpl.class, ProcessedEventGatewayImpl.class})
class MensageriaGatewaysIT extends PostgresContainerSupport {

    static final LocalDateTime T0 = LocalDateTime.of(2026, 10, 22, 14, 0);

    @Autowired OutboxGatewayImpl outbox;
    @Autowired ProcessedEventGatewayImpl processados;

    @Test
    void registraEventoProcessadoApenasUmaVez() {
        UUID id = UUID.randomUUID();

        assertThat(processados.registrar(id, T0)).isTrue();
        assertThat(processados.registrar(id, T0)).isFalse();
        assertThat(processados.registrar(UUID.randomUUID(), T0)).isTrue();
    }

    @Test
    void outboxGuardaPayloadJsonEBuscaPendentesEmOrdemDeCriacao() {
        outbox.salvar(OutboxEvent.criar("EXECUCAO_FINALIZADA", 501L, "{\"osId\":501}", T0.plusMinutes(2)));
        outbox.salvar(OutboxEvent.criar("EXECUCAO_INICIADA", 501L, "{\"osId\":501,\"mecanicoId\":2}", T0.plusMinutes(1)));

        List<OutboxEvent> pendentes = outbox.buscarPendentesComLock(10);

        assertThat(pendentes).extracting(OutboxEvent::getType).containsSubsequence("EXECUCAO_INICIADA", "EXECUCAO_FINALIZADA");
        assertThat(pendentes.stream().filter(e -> e.getType().equals("EXECUCAO_INICIADA")).findFirst().orElseThrow()
                .getPayload()).contains("\"mecanicoId\"");
    }

    @Test
    void eventoPublicadoSaiDosPendentesERespeitaOLimite() {
        OutboxEvent a = outbox.salvar(OutboxEvent.criar("A", 601L, "{}", T0));
        outbox.salvar(OutboxEvent.criar("B", 602L, "{}", T0.plusSeconds(1)));
        a.marcarPublicado(T0.plusMinutes(5));
        outbox.salvar(a);

        assertThat(outbox.buscarPendentesComLock(10)).extracting(OutboxEvent::getId).doesNotContain(a.getId());
        assertThat(outbox.buscarPendentesComLock(1)).hasSize(1);
    }
}