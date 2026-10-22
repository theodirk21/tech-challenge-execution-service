package com.fiap.techchallenge.execution.servico.usecase;

import com.fiap.techchallenge.execution.servico.domain.Servico;
import com.fiap.techchallenge.execution.servico.domain.ServicoStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ServicoInteractorTest {

    @Mock
    ServicoGateway gateway;

    ServicoInteractor interactor;

    @BeforeEach
    void setUp() {
        interactor = new ServicoInteractor(gateway);
        when(gateway.salvar(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void criaUmServicoPorTipo() {
        interactor.criarParaOs(1L, List.of(3L, 4L));

        verify(gateway, times(2)).salvar(any());
    }

    @Test
    void iniciaEFinalizaConformeStatus() {
        Servico s = Servico.criar(3L, 1L);
        when(gateway.listarPorOsId(1L)).thenReturn(List.of(s));

        interactor.iniciarDaOs(1L);
        assertThat(s.getStatus()).isEqualTo(ServicoStatus.EM_EXECUCAO);

        interactor.finalizarDaOs(1L);
        assertThat(s.getStatus()).isEqualTo(ServicoStatus.FINALIZADO);
    }

    @Test
    void cancelaSomentePendentes() {
        Servico pendente = Servico.criar(3L, 1L);
        Servico emExecucao = Servico.criar(4L, 1L);
        emExecucao.iniciar();
        when(gateway.listarPorOsId(1L)).thenReturn(List.of(pendente, emExecucao));

        interactor.cancelarPendentesDaOs(1L);

        assertThat(pendente.getStatus()).isEqualTo(ServicoStatus.DELETADO);
        assertThat(emExecucao.getStatus()).isEqualTo(ServicoStatus.EM_EXECUCAO);
        when(gateway.listarPorOsId(1L)).thenReturn(List.of(pendente));
        assertThat(interactor.listarPorOs(1L)).hasSize(1);
    }
}