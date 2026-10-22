package com.fiap.techchallenge.execution.fila.domain;

import com.fiap.techchallenge.execution.exception.ConflictException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ItemFilaTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 22, 14, 0);

    private ItemFila novo() {
        return ItemFila.criar(10L, "Barulho no freio", "ABC1D23", "Gol", AGORA);
    }

    private ItemFila naFila() {
        ItemFila item = novo();
        item.iniciarDiagnostico(1L);
        item.concluirDiagnostico();
        item.confirmarPagamento();
        return item;
    }

    @Test
    void criaItemAguardandoDiagnostico() {
        ItemFila item = novo();

        assertThat(item.getStatus()).isEqualTo(StatusFila.AGUARDANDO_DIAGNOSTICO);
        assertThat(item.getOsId()).isEqualTo(10L);
        assertThat(item.getSolicitacao()).isEqualTo("Barulho no freio");
        assertThat(item.getVeiculoPlaca()).isEqualTo("ABC1D23");
        assertThat(item.getEntrouEm()).isEqualTo(AGORA);
        assertThat(item.podeSerRetiradoDaFila()).isFalse();
    }

    @Test
    void fluxoFelizPercorreTodosOsStatus() {
        ItemFila item = novo();

        item.iniciarDiagnostico(1L);
        assertThat(item.getStatus()).isEqualTo(StatusFila.EM_DIAGNOSTICO);
        assertThat(item.getMecanicoId()).isEqualTo(1L);

        item.concluirDiagnostico();
        assertThat(item.getStatus()).isEqualTo(StatusFila.AGUARDANDO_PAGAMENTO);

        item.confirmarPagamento();
        assertThat(item.getStatus()).isEqualTo(StatusFila.NA_FILA);
        assertThat(item.podeSerRetiradoDaFila()).isTrue();

        item.iniciarExecucao(2L, AGORA.plusHours(1));
        assertThat(item.getStatus()).isEqualTo(StatusFila.EM_EXECUCAO);
        assertThat(item.getMecanicoId()).isEqualTo(2L);
        assertThat(item.getIniciouEm()).isEqualTo(AGORA.plusHours(1));

        item.finalizar(AGORA.plusHours(3));
        assertThat(item.getStatus()).isEqualTo(StatusFila.FINALIZADA);
        assertThat(item.getFinalizouEm()).isEqualTo(AGORA.plusHours(3));
    }

    @Test
    void naoPodeIniciarExecucaoSemPagamento() {
        ItemFila item = novo();
        item.iniciarDiagnostico(1L);
        item.concluirDiagnostico();

        assertThatThrownBy(() -> item.iniciarExecucao(1L, AGORA))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("AGUARDANDO_PAGAMENTO")
                .hasMessageContaining("NA_FILA");
    }

    @Test
    void naoPodeConcluirDiagnosticoSemIniciar() {
        assertThatThrownBy(() -> novo().concluirDiagnostico()).isInstanceOf(ConflictException.class);
    }

    @Test
    void naoPodeConfirmarPagamentoDuasVezes() {
        ItemFila item = naFila();

        assertThatThrownBy(item::confirmarPagamento).isInstanceOf(ConflictException.class);
    }

    @Test
    void naoPodeFinalizarSemEstarEmExecucao() {
        assertThatThrownBy(() -> naFila().finalizar(AGORA)).isInstanceOf(ConflictException.class);
    }

    @Test
    void falhaNaExecucaoCompensaEEncerra() {
        ItemFila item = naFila();
        item.iniciarExecucao(1L, AGORA);

        item.falhar(AGORA.plusHours(1));

        assertThat(item.getStatus()).isEqualTo(StatusFila.FALHOU);
        assertThat(item.getFinalizouEm()).isEqualTo(AGORA.plusHours(1));
    }

    @Test
    void falhaEmItemNaFilaEPermitida() {
        ItemFila item = naFila();

        item.falhar(AGORA);

        assertThat(item.getStatus()).isEqualTo(StatusFila.FALHOU);
    }

    @Test
    void naoPodeFalharAntesDoPagamento() {
        ItemFila item = novo();

        assertThatThrownBy(() -> item.falhar(AGORA)).isInstanceOf(ConflictException.class);
    }

    @Test
    void cancelaEmQualquerEstadoNaoTerminal() {
        ItemFila aguardando = novo();
        aguardando.cancelar(AGORA);
        ItemFila executando = naFila();
        executando.iniciarExecucao(1L, AGORA);
        executando.cancelar(AGORA);

        assertThat(aguardando.getStatus()).isEqualTo(StatusFila.CANCELADA);
        assertThat(executando.getStatus()).isEqualTo(StatusFila.CANCELADA);
    }

    @Test
    void naoCancelaItemJaEncerrado() {
        ItemFila item = naFila();
        item.iniciarExecucao(1L, AGORA);
        item.finalizar(AGORA);

        assertThatThrownBy(() -> item.cancelar(AGORA))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("FINALIZADA");
    }

    @Test
    void statusTerminaisSaoIdentificados() {
        assertThat(StatusFila.FINALIZADA.isTerminal()).isTrue();
        assertThat(StatusFila.FALHOU.isTerminal()).isTrue();
        assertThat(StatusFila.CANCELADA.isTerminal()).isTrue();
        assertThat(StatusFila.NA_FILA.isTerminal()).isFalse();
    }
}
