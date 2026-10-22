package com.fiap.techchallenge.execution.observabilidade;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ExecucaoEventLoggerTest {

    static final Instant T = Instant.parse("2026-10-22T14:00:00Z");

    @Test
    void publicaEventoDeStatusAlterado() {
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);

        new ExecucaoEventLogger(publisher).registrarStatusAlterado(10L, "NA_FILA", "EM_EXECUCAO", 5L, T);

        verify(publisher).publishEvent(any(ExecucaoEventLogger.StatusDaExecucaoAlterado.class));
    }

    @Test
    void logaStatusAlterado() {
        var logger = new ExecucaoEventLogger(mock(ApplicationEventPublisher.class));

        assertThatCode(() -> logger.registrarLogStatusAlterado(
                new ExecucaoEventLogger.StatusDaExecucaoAlterado(10L, "NA_FILA", "EM_EXECUCAO", 5L, T)))
                .doesNotThrowAnyException();
    }

    @Test
    void logaFalhaTecnicaENaoTecnicaComESemOsId() {
        var logger = new ExecucaoEventLogger(mock(ApplicationEventPublisher.class));

        assertThatCode(() -> {
            logger.registrarFalhaProcessamento(10L, "outbox_publish", "IllegalStateException", T, true);
            logger.registrarFalhaProcessamento(null, "listener", "BadRequestException", T, false);
        }).doesNotThrowAnyException();
    }
}