package com.fiap.techchallenge.execution.mensageria.usecase;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ProcessedEventGateway {

    /** Registra o evento como processado; devolve false se ele já tinha sido registrado (mensagem repetida). */
    boolean registrar(UUID eventId, LocalDateTime agora);
}