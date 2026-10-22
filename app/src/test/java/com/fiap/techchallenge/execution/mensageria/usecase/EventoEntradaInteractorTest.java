package com.fiap.techchallenge.execution.mensageria.usecase;

import com.fiap.techchallenge.execution.fila.usecase.FilaUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EventoEntradaInteractorTest {

    @Mock ProcessedEventGateway processados;
    @Mock FilaUseCase fila;

    EventoEntradaInteractor interactor;

    final UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        interactor = new EventoEntradaInteractor(processados, fila,
                Clock.fixed(Instant.parse("2026-10-22T14:00:00Z"), ZoneOffset.UTC));
    }

    private void primeiraVez(boolean primeira) {
        when(processados.registrar(any(), any())).thenReturn(primeira);
    }

    @Test
    void osCriadaRegistraItemNaFila() {
        primeiraVez(true);

        interactor.processarOsCriada(id, 10L, "freio", "ABC1D23", "Gol");

        verify(fila).registrarOsCriada(10L, "freio", "ABC1D23", "Gol");
    }

    @Test
    void pagamentoConfirmadoConfirmaPagamento() {
        primeiraVez(true);

        interactor.processarPagamentoConfirmado(id, 10L);

        verify(fila).confirmarPagamento(10L);
    }

    @Test
    void osCanceladaEPagamentoEstornadoCancelam() {
        primeiraVez(true);

        interactor.processarOsCancelada(id, 10L);
        interactor.processarPagamentoEstornado(UUID.randomUUID(), 11L);

        verify(fila).cancelar(10L);
        verify(fila).cancelar(11L);
    }

    @Test
    void eventoRepetidoEIgnorado() {
        primeiraVez(false);

        interactor.processarOsCriada(id, 10L, "x", "x", "x");
        interactor.processarPagamentoConfirmado(id, 10L);
        interactor.processarOsCancelada(id, 10L);
        interactor.processarPagamentoEstornado(id, 10L);

        verifyNoInteractions(fila);
    }
}