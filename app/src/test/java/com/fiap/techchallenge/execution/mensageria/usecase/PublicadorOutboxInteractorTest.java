package com.fiap.techchallenge.execution.mensageria.usecase;

import com.fiap.techchallenge.execution.mensageria.domain.OutboxEvent;
import com.fiap.techchallenge.execution.observabilidade.ExecucaoEventLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PublicadorOutboxInteractorTest {

    @Mock OutboxGateway outbox;
    @Mock MensagemPublisher publisher;
    @Mock ExecucaoEventLogger logger;

    PublicadorOutboxInteractor interactor;

    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-22T14:00:00Z"), ZoneOffset.UTC);
    static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 22, 14, 0);

    @BeforeEach
    void setUp() {
        interactor = new PublicadorOutboxInteractor(outbox, publisher, logger, CLOCK);
        when(outbox.salvar(any())).thenAnswer(i -> i.getArgument(0));
    }

    private OutboxEvent evento(long osId) {
        return OutboxEvent.criar("EXECUCAO_INICIADA", osId, "{}", AGORA.minusMinutes(1));
    }

    @Test
    void publicaEMarcaComoPublicado() {
        OutboxEvent a = evento(1L);
        OutboxEvent b = evento(2L);
        when(outbox.buscarPendentesComLock(10)).thenReturn(List.of(a, b));

        int publicados = interactor.publicarPendentes(10);

        assertThat(publicados).isEqualTo(2);
        assertThat(a.getPublishedAt()).isEqualTo(AGORA);
        assertThat(b.foiPublicado()).isTrue();
    }

    @Test
    void paraNoPrimeiroErroMantendoOsDemaisPendentes() {
        OutboxEvent a = evento(1L);
        OutboxEvent b = evento(2L);
        OutboxEvent c = evento(3L);
        when(outbox.buscarPendentesComLock(10)).thenReturn(List.of(a, b, c));
        doThrow(new IllegalStateException("sqs fora")).when(publisher).publicar(b);

        int publicados = interactor.publicarPendentes(10);

        assertThat(publicados).isEqualTo(1);
        assertThat(a.foiPublicado()).isTrue();
        assertThat(b.foiPublicado()).isFalse();
        assertThat(c.foiPublicado()).isFalse();
        verify(publisher, never()).publicar(c);
        verify(logger).registrarFalhaProcessamento(eq(2L), eq("outbox_publish"), eq("IllegalStateException"), any(), eq(true));
    }

    @Test
    void semPendentesNaoFazNada() {
        when(outbox.buscarPendentesComLock(10)).thenReturn(List.of());

        assertThat(interactor.publicarPendentes(10)).isZero();
        verify(publisher, never()).publicar(any());
    }
}