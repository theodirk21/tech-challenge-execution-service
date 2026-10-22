package com.fiap.techchallenge.execution.mensageria.adapter.scheduler;

import com.fiap.techchallenge.execution.mensageria.usecase.PublicadorOutboxUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "execution.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class PublicadorOutboxScheduler {

    private final PublicadorOutboxUseCase publicador;
    private final int lote;

    public PublicadorOutboxScheduler(PublicadorOutboxUseCase publicador,
                                     @Value("${execution.outbox.lote:50}") int lote) {
        this.publicador = publicador;
        this.lote = lote;
    }

    @Scheduled(fixedDelayString = "${execution.outbox.intervalo-ms:1000}")
    public void publicar() {
        publicador.publicarPendentes(lote);
    }
}