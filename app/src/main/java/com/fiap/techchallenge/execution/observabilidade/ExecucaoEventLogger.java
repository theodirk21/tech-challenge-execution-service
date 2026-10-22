package com.fiap.techchallenge.execution.observabilidade;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;


@Component
@Slf4j
public class ExecucaoEventLogger {

    private final ApplicationEventPublisher eventPublisher;

    public ExecucaoEventLogger(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void registrarStatusAlterado(Long osId, String statusAnterior, String novoStatus,
                                        long duracaoNoStatusAnteriorEmMs, Instant ocorreuEm) {
        eventPublisher.publishEvent(new StatusDaExecucaoAlterado(osId, statusAnterior, novoStatus,
                duracaoNoStatusAnteriorEmMs, ocorreuEm));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void registrarLogStatusAlterado(StatusDaExecucaoAlterado evento) {
        log.atInfo()
                .addKeyValue("executionEventType", "execution_status_changed")
                .addKeyValue("orderId", evento.osId())
                .addKeyValue("fromStatus", evento.statusAnterior())
                .addKeyValue("toStatus", evento.novoStatus())
                .addKeyValue("durationInPreviousStatusMs", evento.duracaoNoStatusAnteriorEmMs())
                .addKeyValue("occurredAt", evento.ocorreuEm().toString())
                .log("Status da execução alterado");
    }

    public void registrarFalhaProcessamento(Long osId, String operacao, String tipoErro,
                                            Instant ocorreuEm, boolean tecnico) {
        LoggingEventBuilder evento = tecnico ? log.atError() : log.atWarn();
        evento.addKeyValue("executionEventType", "execution_processing_failed");
        if (osId != null) {
            evento.addKeyValue("orderId", osId);
        }
        evento.addKeyValue("operation", operacao)
                .addKeyValue("errorType", tipoErro)
                .addKeyValue("occurredAt", ocorreuEm.toString())
                .log("Falha no processamento da execução");
    }

    public record StatusDaExecucaoAlterado(Long osId, String statusAnterior, String novoStatus,
                                           long duracaoNoStatusAnteriorEmMs, Instant ocorreuEm) {}
}
