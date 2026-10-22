package com.fiap.techchallenge.execution.mensageria.adapter.listener;

import com.fiap.techchallenge.execution.exception.BadRequestException;
import com.fiap.techchallenge.execution.mensageria.usecase.EventoEntradaUseCase;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class EventoEntradaListenerTest {

    final EventoEntradaUseCase eventos = mock(EventoEntradaUseCase.class);
    final EventoEntradaListener listener = new EventoEntradaListener(eventos, JsonMapper.builder().build());
    final UUID id = UUID.randomUUID();

    private String msg(String payload) {
        return "{\"eventId\":\"%s\",\"type\":\"X\",\"version\":1,\"payload\":%s}".formatted(id, payload);
    }

    @Test
    void osCriadaLeVeiculoESolicitacao() {
        listener.aoCriarOs(msg("{\"osId\":10,\"solicitacao\":\"freio\",\"veiculo\":{\"placa\":\"ABC1D23\",\"modelo\":\"Gol\"}}"));

        verify(eventos).processarOsCriada(id, 10L, "freio", "ABC1D23", "Gol");
    }

    @Test
    void osCriadaSemVeiculoNemSolicitacaoPassaNulos() {
        listener.aoCriarOs(msg("{\"osId\":10,\"solicitacao\":null}"));

        verify(eventos).processarOsCriada(id, 10L, null, null, null);
    }

    @Test
    void demaisEventosEncaminhamOsId() {
        listener.aoConfirmarPagamento(msg("{\"osId\":10}"));
        listener.aoCancelarOs(msg("{\"osId\":11}"));
        listener.aoEstornarPagamento(msg("{\"osId\":12}"));

        verify(eventos).processarPagamentoConfirmado(id, 10L);
        verify(eventos).processarOsCancelada(id, 11L);
        verify(eventos).processarPagamentoEstornado(id, 12L);
    }

    @Test
    void mensagemInvalidaLancaBadRequestSemProcessar() {
        assertThatThrownBy(() -> listener.aoCriarOs("não é json")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> listener.aoCriarOs("{\"eventId\":\"nao-uuid\",\"payload\":{\"osId\":1}}"))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> listener.aoCriarOs("{\"eventId\":\"" + id + "\"}")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> listener.aoCriarOs("{\"eventId\":\"" + id + "\",\"payload\":{\"x\":1}}"))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> listener.aoCriarOs("{\"payload\":{\"osId\":1}}")).isInstanceOf(BadRequestException.class);

        verifyNoInteractions(eventos);
    }
}