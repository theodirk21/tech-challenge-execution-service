package com.fiap.techchallenge.execution.fila.adapter.gateway;

import com.fiap.techchallenge.execution.fila.domain.ItemFila;
import com.fiap.techchallenge.execution.fila.domain.StatusFila;
import com.fiap.techchallenge.execution.support.PostgresContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(FilaGatewayImpl.class)
@ActiveProfiles("test")
class FilaGatewayImplIT extends PostgresContainerSupport {

    @Autowired
    FilaGatewayImpl repo;

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 1, 1, 8, 0);

    private ItemFila naFila(long osId, int prioridade, LocalDateTime entrouEm) {
        ItemFila item = ItemFila.criar(osId, "Troca de óleo", "2026-11-08T20:41:06Z", 1L, 1L, "16", entrouEm);
        item.iniciarDiagnostico(1L);
        item.concluirDiagnostico();
        item.confirmarPagamento();
        item.setPrioridade(prioridade);
        return item;
    }

    @Test
    void salvaEBuscaPreservandoTodosOsCampos() {
        ItemFila salvo = repo.salvar(ItemFila.criar(5100L, "Freio", "2026-11-08T20:41:06Z", 1L, 1L, "16", T0));

        assertThat(salvo.getId()).isNotNull();
        ItemFila lido = repo.buscarPorOsId(5100L).orElseThrow();
        assertThat(lido.getStatus()).isEqualTo(StatusFila.AGUARDANDO_DIAGNOSTICO);
        assertThat(lido.getSolicitacao()).isEqualTo("Freio");
        assertThat(lido.getEntrouEm()).isEqualTo(T0);
    }

    @Test
    void persisteTransicaoDeStatus() {
        ItemFila item = repo.salvar(ItemFila.criar(5101L, "Revisão", "2026-11-08T20:41:06Z", 1L, 1L, "16", T0));
        item.iniciarDiagnostico(7L);
        repo.salvar(item);

        ItemFila lido = repo.buscarPorOsIdComLock(5101L).orElseThrow();
        assertThat(lido.getStatus()).isEqualTo(StatusFila.EM_DIAGNOSTICO);
        assertThat(lido.getMecanicoId()).isEqualTo(7L);
    }

    @Test
    void proximoDaFilaRespeitaPrioridadeEDepoisAntiguidade() {
        repo.salvar(naFila(5201L, 0, T0));
        repo.salvar(naFila(5202L, 5, T0.plusMinutes(10)));
        repo.salvar(naFila(5203L, 5, T0.plusMinutes(5)));

        assertThat(repo.proximoDaFilaComLock()).get().extracting(ItemFila::getOsId).isEqualTo(5203L);
    }

    @Test
    void proximoDaFilaIgnoraItensForaDaFila() {
        repo.salvar(ItemFila.criar(5301L, "x", "2026-11-08T20:41:06Z", 1L, 1L, "16", T0));

        assertThat(repo.proximoDaFilaComLock()).isEmpty();
    }

    @Test
    void listaPorStatusOrdenado() {
        repo.salvar(naFila(5401L, 0, T0));
        repo.salvar(naFila(5402L, 9, T0.plusMinutes(1)));

        assertThat(repo.listarPorStatus(StatusFila.NA_FILA)).extracting(ItemFila::getOsId)
                .containsExactly(5402L, 5401L);
    }
}