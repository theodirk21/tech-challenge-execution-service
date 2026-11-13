package com.fiap.techchallenge.execution.observabilidade;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EventoLogger {

    // === LISTENER - RECEBIMENTO DE MENSAGENS ===
    public void logRecebimentoMensagem(String fila, String correlationId,
                                       String eventId, String eventType) {
        log.atInfo()
                .addKeyValue("service", "execution-service")
                .addKeyValue("correlationId", correlationId)
                .addKeyValue("eventId", eventId)
                .addKeyValue("eventType", eventType)
                .addKeyValue("operation", "receber_evento")
                .addKeyValue("outcome", "recebida")
                .log("Mensagem recebida na fila " + fila);
    }

    // === FILA INTERACTOR ===
    public void logOsCriada(Long osId, String correlationId,
                            String veiculoPlaca, String veiculoModelo) {
        log.atInfo()
                .addKeyValue("service", "execution-service")
                .addKeyValue("correlationId", correlationId)
                .addKeyValue("orderId", osId)
                .addKeyValue("operation", "registrar_os")
                .addKeyValue("outcome", "registrada")
                .addKeyValue("veiculoPlaca", veiculoPlaca)
                .addKeyValue("veiculoModelo", veiculoModelo)
                .log("Ordem de serviço registrada na fila");
    }

    public void logConfirmacaoPagamento(Long osId, String correlationId, String outcome, String message) {
        log.atInfo()
                .addKeyValue("service", "execution-service")
                .addKeyValue("correlationId", correlationId)
                .addKeyValue("orderId", osId)
                .addKeyValue("operation", "confirmar_pagamento")
                .addKeyValue("outcome", outcome)  // "confirmado" ou "falha"
                .log(message);
    }

    public static void logJwtInvalido(String erro) {
        log.atWarn()
                .addKeyValue("service", "execution-service")
                .addKeyValue("operation", "validar_jwt")
                .addKeyValue("outcome", "falha")
                .log("JWT inválido ou expirado: " + erro);
    }
}
