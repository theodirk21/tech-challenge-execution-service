package com.fiap.techchallenge.execution.mensageria.usecase;

import com.fiap.techchallenge.execution.mensageria.domain.OutboxEvent;
import com.fiap.techchallenge.execution.observabilidade.ExecucaoEventLogger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;

@Service
public class PublicadorOutboxInteractor implements PublicadorOutboxUseCase {

    private final OutboxGateway outbox;
    private final MensagemPublisher publisher;
    private final ExecucaoEventLogger logger;
    private final Clock clock;

    public PublicadorOutboxInteractor(OutboxGateway outbox, MensagemPublisher publisher,
                                      ExecucaoEventLogger logger, Clock clock) {
        this.outbox = outbox;
        this.publisher = publisher;
        this.logger = logger;
        this.clock = clock;
    }

    @Override
    @Transactional
    public int publicarPendentes(int lote) {
        int publicados = 0;
        for (OutboxEvent evento : outbox.buscarPendentesComLock(lote)) {
            try {
                publisher.publicar(evento);
            } catch (RuntimeException e) {
                logger.registrarFalhaProcessamento(evento.getAggregateId(), "outbox_publish",
                        e.getClass().getSimpleName(), Instant.now(clock), true);
                break;
            }
            evento.marcarPublicado(LocalDateTime.now(clock));
            outbox.salvar(evento);
            publicados++;
        }
        return publicados;
    }
}