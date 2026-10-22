package com.fiap.techchallenge.execution.mensageria.adapter.listener;

import com.fiap.techchallenge.execution.exception.BadRequestException;
import com.fiap.techchallenge.execution.mensageria.usecase.EventoEntradaUseCase;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

@Component
public class EventoEntradaListener {

    private final EventoEntradaUseCase eventos;
    private final JsonMapper mapper;

    public EventoEntradaListener(EventoEntradaUseCase eventos, JsonMapper mapper) {
        this.eventos = eventos;
        this.mapper = mapper;
    }

    @SqsListener("${execution.sqs.os-criada}")
    public void aoCriarOs(String mensagem) {
        EnvelopeEntrada e = ler(mensagem);
        eventos.processarOsCriada(e.eventId(), e.osId(), e.texto("solicitacao"),
                e.texto("veiculo", "placa"), e.texto("veiculo", "modelo"));
    }

    @SqsListener("${execution.sqs.pagamento-confirmado}")
    public void aoConfirmarPagamento(String mensagem) {
        EnvelopeEntrada e = ler(mensagem);
        eventos.processarPagamentoConfirmado(e.eventId(), e.osId());
    }

    @SqsListener("${execution.sqs.os-cancelada}")
    public void aoCancelarOs(String mensagem) {
        EnvelopeEntrada e = ler(mensagem);
        eventos.processarOsCancelada(e.eventId(), e.osId());
    }

    @SqsListener("${execution.sqs.pagamento-estornado}")
    public void aoEstornarPagamento(String mensagem) {
        EnvelopeEntrada e = ler(mensagem);
        eventos.processarPagamentoEstornado(e.eventId(), e.osId());
    }

    private EnvelopeEntrada ler(String mensagem) {
        try {
            var raiz = mapper.readTree(mensagem);
            UUID eventId = UUID.fromString(raiz.path("eventId").asString());
            var payload = raiz.path("payload");
            if (payload.isMissingNode() || !payload.hasNonNull("osId")) {
                throw new BadRequestException("Mensagem sem payload.osId!");
            }
            return new EnvelopeEntrada(eventId, raiz.path("type").asString(), payload);
        } catch (IllegalArgumentException | tools.jackson.core.JacksonException e) {
            throw new BadRequestException("Mensagem inválida: " + e.getMessage());
        }
    }
}