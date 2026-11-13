package com.fiap.techchallenge.execution.observabilidade;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EventoLogger - Testes de Logging")
class EventoLoggerContentTest {

    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;
    private EventoLogger eventoLogger;
    private static final UUID EVENT_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    @BeforeEach
    void setUp() {
        eventoLogger = new EventoLogger();

        logger = (Logger) LoggerFactory.getLogger(EventoLogger.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        logger.setLevel(Level.INFO);
    }

    @Test
    @DisplayName("Deve logar recebimento de mensagem")
    void verificarFieldsRecebimento() {
        String correlationId = "corr-007";
        String eventType = "OS_CRIADA";

        eventoLogger.logRecebimentoMensagem("os-execution", correlationId, EVENT_ID.toString(), eventType);

        assertThat(listAppender.list).isNotEmpty();
        ILoggingEvent logEvent = listAppender.list.get(0);

        assertThat(logEvent.getLevel()).isEqualTo(Level.INFO);
        assertThat(logEvent.getFormattedMessage())
                .contains("Mensagem recebida na fila os-execution");
    }

    @Test
    @DisplayName("Deve logar confirmação de pagamento")
    void verificarFieldsConfirmacaoPagamento() {
        Long osId = 127L;
        String correlationId = "corr-008";
        String outcome = "confirmado";

        eventoLogger.logConfirmacaoPagamento(osId, correlationId, outcome,
                "Pagamento confirmado com sucesso");

        assertThat(listAppender.list).isNotEmpty();
        ILoggingEvent logEvent = listAppender.list.get(0);

        assertThat(logEvent.getLevel()).isEqualTo(Level.INFO);
        assertThat(logEvent.getFormattedMessage())
                .contains("Pagamento confirmado com sucesso");
    }

    @Test
    @DisplayName("Deve logar JWT inválido com nível WARN")
    void verificarLevelJwtInvalido() {
        EventoLogger.logJwtInvalido("Token expirado");

        assertThat(listAppender.list).isNotEmpty();
        ILoggingEvent logEvent = listAppender.list.get(0);

        assertThat(logEvent.getLevel()).isEqualTo(Level.WARN);
        assertThat(logEvent.getFormattedMessage())
                .contains("JWT inválido ou expirado: Token expirado");
    }

    @Test
    @DisplayName("Deve logar OS criada")
    void verificarLogOsCriada() {
        Long osId = 100L;
        String correlationId = "corr-009";

        eventoLogger.logOsCriada(osId, correlationId, "ABC-1234", "Civic");

        assertThat(listAppender.list).isNotEmpty();
        ILoggingEvent logEvent = listAppender.list.get(0);

        assertThat(logEvent.getLevel()).isEqualTo(Level.INFO);
        assertThat(logEvent.getFormattedMessage())
                .contains("Ordem de serviço registrada na fila");
    }
}