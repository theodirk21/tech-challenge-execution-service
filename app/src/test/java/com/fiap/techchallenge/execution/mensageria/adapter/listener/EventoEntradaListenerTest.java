package com.fiap.techchallenge.execution.mensageria.adapter.listener;

import com.fiap.techchallenge.execution.exception.BadRequestException;
import com.fiap.techchallenge.execution.mensageria.usecase.EventoEntradaUseCase;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class EventoEntradaListenerTest {

    final EventoEntradaUseCase eventos = mock(EventoEntradaUseCase.class);
    final EventoEntradaListener listener = new EventoEntradaListener(eventos, JsonMapper.builder().build());
    final UUID id = UUID.randomUUID();

    private String envOsExecution(String type, String payload) {
        return "{\"eventId\":\"%s\",\"type\":\"%s\",\"version\":1,\"occurredAt\":\"2026-11-08T20:41:06Z\",\"correlationId\":\"16\",\"payload\":%s}".formatted(id, type, payload);
    }

    private String envBillingExecution(String type, String payload) {
        return "{\"eventId\":\"%s\",\"type\":\"%s\",\"version\":1,\"occurredAt\":\"2026-11-08T20:41:06Z\",\"correlationId\":\"16\",\"payload\":%s}".formatted(id, type, payload);
    }

    @Test
    void osCriadaRoteiaPraOsExecutionComTodosOsCampos() {
        var msg = envOsExecution("OS_CRIADA", "{\"osId\":10,\"solicitacao\":\"Trocar óleo\",\"criadaEm\":\"2026-11-08T20:41:06.266238836Z\",\"clienteId\":1,\"veiculoId\":1}");
        listener.consumirFilaOsExecution(msg);

        verify(eventos).processarDoOsExecution(any(EnvelopeEntrada.class));
    }

    @Test
    void osCanceladaRoteiaPraOsExecution() {
        var msg = envOsExecution("OS_CANCELADA", "{\"osId\":10}");
        listener.consumirFilaOsExecution(msg);

        verify(eventos).processarDoOsExecution(any(EnvelopeEntrada.class));
    }

    @Test
    void pagamentoConfirmadoRoteiaPraBillingExecution() {
        var msg = envBillingExecution("PAGAMENTO_CONFIRMADO", "{\"osId\":10}");
        listener.consumirFilaBillingExecution(msg);

        verify(eventos).processarDoBillingExecution(any(EnvelopeEntrada.class));
    }

    @Test
    void pagamentoFalhDouEstornadoRoteiaParaBillingExecution() {
        var msgFalhou = envBillingExecution("PAGAMENTO_FALHOU", "{\"osId\":10}");
        var msgEstornado = envBillingExecution("PAGAMENTO_ESTORNADO", "{\"osId\":11}");

        listener.consumirFilaBillingExecution(msgFalhou);
        listener.consumirFilaBillingExecution(msgEstornado);

        verify(eventos).processarDoBillingExecution(any(EnvelopeEntrada.class));
    }

    @Test
    void mensagemInvalidaLancaBadRequest() {
        assertThatThrownBy(() -> listener.consumirFilaOsExecution("não é json")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> listener.consumirFilaOsExecution("{\"eventId\":\"nao-uuid\"}")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> listener.consumirFilaOsExecution("{\"eventId\":\"" + id + "\"}")).isInstanceOf(BadRequestException.class);

        verifyNoInteractions(eventos);
    }

    @Test
    void payloadAusenterLancaBadRequest() {
        var semPayload = "{\"eventId\":\"%s\",\"type\":\"OS_CRIADA\",\"version\":1}".formatted(id);
        assertThatThrownBy(() -> listener.consumirFilaOsExecution(semPayload)).isInstanceOf(BadRequestException.class);

        verifyNoInteractions(eventos);
    }
}