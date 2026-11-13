package com.fiap.techchallenge.execution.mensageria.usecase;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ExecucaoEventComponentTest {

    static final Instant T = Instant.parse("2026-10-22T14:00:00Z");

    @Test
    void publicaEventoDeStatusAlterado() {
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);

        new ExecucaoEventComponent(publisher).registrarStatusAlterado(10L, "NA_FILA", "EM_EXECUCAO", 5L, T);

        verify(publisher).publishEvent(any(ExecucaoEventComponent.StatusDaExecucaoAlterado.class));
    }

    @Test
    void logaStatusAlterado() {
        var logger = new ExecucaoEventComponent(mock(ApplicationEventPublisher.class));

        assertThatCode(() -> logger.registrarLogStatusAlterado(
                new ExecucaoEventComponent.StatusDaExecucaoAlterado(10L, "NA_FILA", "EM_EXECUCAO", 5L, T)))
                .doesNotThrowAnyException();
    }

    @Test
    void logaFalhaTecnicaENaoTecnicaComESemOsId() {
        var logger = new ExecucaoEventComponent(mock(ApplicationEventPublisher.class));

        assertThatCode(() -> {
            logger.registrarFalhaProcessamento(10L, "outbox_publish", "IllegalStateException", T, true);
            logger.registrarFalhaProcessamento(null, "listener", "BadRequestException", T, false);
        }).doesNotThrowAnyException();
    }
}