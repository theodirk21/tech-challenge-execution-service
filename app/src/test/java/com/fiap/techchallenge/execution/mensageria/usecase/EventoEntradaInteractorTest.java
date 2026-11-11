package com.fiap.techchallenge.execution.mensageria.usecase;

import com.fiap.techchallenge.execution.exception.BadRequestException;
import com.fiap.techchallenge.execution.fila.usecase.FilaUseCase;
import com.fiap.techchallenge.execution.mensageria.adapter.listener.EnvelopeEntrada;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    final JsonMapper mapper = JsonMapper.builder().build();

    @BeforeEach
    void setUp() {
        interactor = new EventoEntradaInteractor(processados, fila,
                Clock.fixed(Instant.parse("2026-10-22T14:00:00Z"), ZoneOffset.UTC));
    }

    private void primeiraVez(boolean primeira) {
        when(processados.registrar(any(), any())).thenReturn(primeira);
    }

    private EnvelopeEntrada envOsExecution(String type, String payloadJson) {
        String json = "{\"eventId\":\"%s\",\"type\":\"%s\",\"version\":1,\"occurredAt\":\"2026-11-08T20:41:06Z\",\"correlationId\":\"16\",\"payload\":%s}".formatted(id, type, payloadJson);
        var root = mapper.readTree(json);
        return new EnvelopeEntrada(
            UUID.fromString(root.path("eventId").asString()),
            root.path("type").asString(),
            root.path("payload"),
            root.path("version").asInt(),
            root.path("occurredAt").asString(),
            root.path("correlationId").asString()
        );
    }

    @Test
    void osCriadaRegistraItemNaFilaComTodosCampos() {
        primeiraVez(true);
        var env = envOsExecution("OS_CRIADA", "{\"osId\":10,\"solicitacao\":\"Trocar óleo\",\"criadaEm\":\"2026-11-08T20:41:06Z\",\"clienteId\":1,\"veiculoId\":1,\"veiculo\":{\"placa\":\"ABC1D23\",\"modelo\":\"Gol\"}}");

        interactor.processarDoOsExecution(env);

        verify(fila).registrarOsCriada(10L, "Trocar óleo", "2026-11-08T20:41:06Z", 1L, 1L, "16", "ABC1D23", "Gol");
    }

    @Test
    void osCanceladaChama() {
        primeiraVez(true);
        var env = envOsExecution("OS_CANCELADA", "{\"osId\":10}");

        interactor.processarDoOsExecution(env);

        verify(fila).cancelar(10L);
    }

    @Test
    void pagamentoConfirmadoConfirmaPagamento() {
        primeiraVez(true);
        var env = envOsExecution("PAGAMENTO_CONFIRMADO", "{\"osId\":10}");

        interactor.processarDoBillingExecution(env);

        verify(fila).confirmarPagamento(10L);
    }

    @Test
    void pagamentoFalhourChamaFalharExecucao() {
        primeiraVez(true);
        var env = envOsExecution("PAGAMENTO_FALHOU", "{\"osId\":10}");

        interactor.processarDoBillingExecution(env);

        verify(fila).falharExecucao(10L, "Pagamento falhou no serviço de billing", "PAGAMENTO_FALHOU");
    }

    @Test
    void pagamentoEstornadoCancela() {
        primeiraVez(true);
        var env = envOsExecution("PAGAMENTO_ESTORNADO", "{\"osId\":10}");

        interactor.processarDoBillingExecution(env);

        verify(fila).cancelar(10L);
    }

    @Test
    void eventoRepetidoEIgnorado() {
        primeiraVez(false);
        var env1 = envOsExecution("OS_CRIADA", "{\"osId\":10,\"solicitacao\":\"x\",\"criadaEm\":\"2026-11-08T20:41:06Z\",\"clienteId\":1,\"veiculoId\":1}");
        var env2 = envOsExecution("PAGAMENTO_CONFIRMADO", "{\"osId\":10}");

        interactor.processarDoOsExecution(env1);
        interactor.processarDoBillingExecution(env2);

        verifyNoInteractions(fila);
    }

    @Test
    void tipoDesconhecidoEmOsExecutionLancaErro() {
        primeiraVez(true);
        var env = envOsExecution("TIPO_INVALIDO", "{\"osId\":10}");

        assertThatThrownBy(() -> interactor.processarDoOsExecution(env))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("desconhecido em os_execution");
    }

    @Test
    void tipoDesconhecidoEmBillingExecutionLancaErro() {
        primeiraVez(true);
        var env = envOsExecution("TIPO_INVALIDO", "{\"osId\":10}");

        assertThatThrownBy(() -> interactor.processarDoBillingExecution(env))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("desconhecido em billing_execution");
    }
}