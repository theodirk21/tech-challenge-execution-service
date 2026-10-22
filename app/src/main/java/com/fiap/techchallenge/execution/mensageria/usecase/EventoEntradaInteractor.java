package com.fiap.techchallenge.execution.mensageria.usecase;

import com.fiap.techchallenge.execution.fila.usecase.FilaUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * O registro em processed_event e a alteração da fila ficam na mesma transação: se o tratamento falhar,
 * o registro é desfeito e a mensagem pode ser reentregue (e vai para a DLQ após as tentativas da fila).
 */
@Service
@Transactional
public class EventoEntradaInteractor implements EventoEntradaUseCase {

    private final ProcessedEventGateway processados;
    private final FilaUseCase fila;
    private final Clock clock;

    public EventoEntradaInteractor(ProcessedEventGateway processados, FilaUseCase fila, Clock clock) {
        this.processados = processados;
        this.fila = fila;
        this.clock = clock;
    }

    @Override
    public void processarOsCriada(UUID eventId, Long osId, String solicitacao, String veiculoPlaca, String veiculoModelo) {
        if (repetido(eventId)) {
            return;
        }
        fila.registrarOsCriada(osId, solicitacao, veiculoPlaca, veiculoModelo);
    }

    @Override
    public void processarPagamentoConfirmado(UUID eventId, Long osId) {
        if (repetido(eventId)) {
            return;
        }
        fila.confirmarPagamento(osId);
    }

    @Override
    public void processarOsCancelada(UUID eventId, Long osId) {
        if (repetido(eventId)) {
            return;
        }
        fila.cancelar(osId);
    }

    @Override
    public void processarPagamentoEstornado(UUID eventId, Long osId) {
        if (repetido(eventId)) {
            return;
        }
        fila.cancelar(osId);
    }

    private boolean repetido(UUID eventId) {
        return !processados.registrar(eventId, LocalDateTime.now(clock));
    }
}