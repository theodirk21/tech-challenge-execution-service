package com.fiap.techchallenge.execution.servico.domain;

import com.fiap.techchallenge.execution.exception.ServicoBadStatusException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServicoTest {

    @Test
    void criaAguardandoInicio() {
        Servico s = Servico.criar(3L, 10L);

        assertThat(s.getStatus()).isEqualTo(ServicoStatus.AGUARDANDO_INICIO);
        assertThat(s.getTipoServicoId()).isEqualTo(3L);
        assertThat(s.getOsId()).isEqualTo(10L);
    }

    @Test
    void cicloCompletoPreencheDatas() {
        Servico s = Servico.criar(3L, 10L);

        s.iniciar();
        assertThat(s.getStatus()).isEqualTo(ServicoStatus.EM_EXECUCAO);
        assertThat(s.getDataInicio()).isNotNull();

        s.finalizar();
        assertThat(s.getStatus()).isEqualTo(ServicoStatus.FINALIZADO);
        assertThat(s.getDataFim()).isNotNull();
    }

    @Test
    void naoIniciaDuasVezesNemFinalizaSemIniciar() {
        Servico s = Servico.criar(3L, 10L);
        assertThatThrownBy(s::finalizar).isInstanceOf(ServicoBadStatusException.class);

        s.iniciar();
        assertThatThrownBy(s::iniciar).isInstanceOf(ServicoBadStatusException.class);
    }

    @Test
    void deletarSoQuandoNaoEstaEmExecucao() {
        Servico emExecucao = Servico.criar(3L, 10L);
        emExecucao.iniciar();
        assertThatThrownBy(emExecucao::deletar).isInstanceOf(ServicoBadStatusException.class);

        Servico pendente = Servico.criar(3L, 10L);
        pendente.deletar();
        assertThat(pendente.getStatus()).isEqualTo(ServicoStatus.DELETADO);
    }

    @Test
    void atualizaApenasCamposInformados() {
        Servico s = Servico.criar(3L, 10L);

        s.atualizar(null, null);
        assertThat(s.getOsId()).isEqualTo(10L);
        assertThat(s.getTipoServicoId()).isEqualTo(3L);

        s.atualizar(11L, null);
        assertThat(s.getOsId()).isEqualTo(11L);
        assertThat(s.getTipoServicoId()).isEqualTo(3L);

        s.atualizar(null, 4L);
        assertThat(s.getOsId()).isEqualTo(11L);
        assertThat(s.getTipoServicoId()).isEqualTo(4L);
    }

    @Test
    void naoAtualizaServicoFinalizado() {
        Servico s = Servico.criar(3L, 10L);
        s.iniciar();
        s.finalizar();

        assertThatThrownBy(() -> s.atualizar(11L, 4L)).isInstanceOf(ServicoBadStatusException.class);
    }

    @Test
    void settersEConstrutorVazio() {
        Servico s = new Servico();
        s.setId(1L);
        s.setOsId(2L);
        s.setTipoServicoId(3L);
        s.setStatus(ServicoStatus.EM_EXECUCAO);
        s.setDataInicio(java.time.LocalDateTime.of(2026, 1, 1, 8, 0));
        s.setDataFim(java.time.LocalDateTime.of(2026, 1, 1, 9, 0));

        assertThat(s.getId()).isEqualTo(1L);
        assertThat(s.getOsId()).isEqualTo(2L);
        assertThat(s.getTipoServicoId()).isEqualTo(3L);
        assertThat(s.getStatus()).isEqualTo(ServicoStatus.EM_EXECUCAO);
        assertThat(s.getDataInicio()).isBefore(s.getDataFim());
    }
}