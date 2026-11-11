package com.fiap.techchallenge.execution.mensageria.usecase;

import com.fiap.techchallenge.execution.exception.BadRequestException;
import com.fiap.techchallenge.execution.fila.usecase.FilaUseCase;
import com.fiap.techchallenge.execution.mensageria.adapter.listener.EnvelopeEntrada;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

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
    public void processarDoOsExecution(EnvelopeEntrada envelope) {
        if (repetido(envelope.eventId())) {
            return;
        }
        
        switch (envelope.type()) {
            case "OS_CRIADA" -> fila.registrarOsCriada(
                envelope.osId(),
                envelope.solicitacao(),
                envelope.criadaEm(),
                envelope.clienteId(),
                envelope.veiculoId(),
                envelope.correlationId(),
                envelope.veiculoPlaca(),
                envelope.veiculoModelo()
            );
            case "OS_CANCELADA" -> fila.cancelar(envelope.osId());
            default -> throw new BadRequestException("Tipo de evento desconhecido em os_execution: " + envelope.type());
        }
    }

    @Override
    public void processarDoBillingExecution(EnvelopeEntrada envelope) {
        if (repetido(envelope.eventId())) {
            return;
        }
        
        switch (envelope.type()) {
            case "PAGAMENTO_CONFIRMADO" -> fila.confirmarPagamento(envelope.osId());
            case "PAGAMENTO_FALHOU" -> fila.falharExecucao(envelope.osId(), "Pagamento falhou no serviço de billing", "PAGAMENTO_FALHOU");
            case "PAGAMENTO_ESTORNADO" -> fila.cancelar(envelope.osId());
            default -> throw new BadRequestException("Tipo de evento desconhecido em billing_execution: " + envelope.type());
        }
    }

    private boolean repetido(java.util.UUID eventId) {
        return !processados.registrar(eventId, LocalDateTime.now(clock));
    }
}