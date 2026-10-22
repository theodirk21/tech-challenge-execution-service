package com.fiap.techchallenge.execution.fila.adapter.gateway;

import com.fiap.techchallenge.execution.fila.domain.ItemFila;
import com.fiap.techchallenge.execution.fila.domain.StatusFila;
import com.fiap.techchallenge.execution.support.PostgresContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(FilaGatewayImpl.class)
class FilaGatewayImplIT extends PostgresContainerSupport {

    @Autowired
    FilaGatewayImpl repo;

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 1, 1, 8, 0);

    private ItemFila naFila(long osId, int prioridade, LocalDateTime entrouEm) {
        ItemFila item = ItemFila.criar(osId, "Troca de óleo", "ABC1D23", "Gol", entrouEm);
        item.iniciarDiagnostico(1L);
        item.concluirDiagnostico();
        item.confirmarPagamento();
        item.setPrioridade(prioridade);
        return item;
    }

    @Test
    void salvaEBuscaPreservandoTodosOsCampos() {
        ItemFila salvo = repo.salvar(ItemFila.criar(100L, "Freio", "XYZ9K88", "Onix", T0));

        assertThat(salvo.getId()).isNotNull();
        ItemFila lido = repo.buscarPorOsId(100L).orElseThrow();
        assertThat(lido.getStatus()).isEqualTo(StatusFila.AGUARDANDO_DIAGNOSTICO);
        assertThat(lido.getSolicitacao()).isEqualTo("Freio");
        assertThat(lido.getVeiculoPlaca()).isEqualTo("XYZ9K88");
        assertThat(lido.getVeiculoModelo()).isEqualTo("Onix");
        assertThat(lido.getEntrouEm()).isEqualTo(T0);
    }

    @Test
    void persisteTransicaoDeStatus() {
        ItemFila item = repo.salvar(ItemFila.criar(101L, "Revisão", "AAA1A11", "Uno", T0));
        item.iniciarDiagnostico(7L);
        repo.salvar(item);

        ItemFila lido = repo.buscarPorOsIdComLock(101L).orElseThrow();
        assertThat(lido.getStatus()).isEqualTo(StatusFila.EM_DIAGNOSTICO);
        assertThat(lido.getMecanicoId()).isEqualTo(7L);
    }

    @Test
    void proximoDaFilaRespeitaPrioridadeEDepoisAntiguidade() {
        repo.salvar(naFila(201L, 0, T0));
        repo.salvar(naFila(202L, 5, T0.plusMinutes(10)));
        repo.salvar(naFila(203L, 5, T0.plusMinutes(5)));

        assertThat(repo.proximoDaFilaComLock()).get().extracting(ItemFila::getOsId).isEqualTo(203L);
    }

    @Test
    void proximoDaFilaIgnoraItensForaDaFila() {
        repo.salvar(ItemFila.criar(301L, "x", "AAA1A11", "Uno", T0));

        assertThat(repo.proximoDaFilaComLock()).isEmpty();
    }

    @Test
    void listaPorStatusOrdenado() {
        repo.salvar(naFila(401L, 0, T0));
        repo.salvar(naFila(402L, 9, T0.plusMinutes(1)));

        assertThat(repo.listarPorStatus(StatusFila.NA_FILA)).extracting(ItemFila::getOsId)
                .containsExactly(402L, 401L);
    }
}
