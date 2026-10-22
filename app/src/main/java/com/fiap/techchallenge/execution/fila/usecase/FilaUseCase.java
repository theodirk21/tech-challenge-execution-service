package com.fiap.techchallenge.execution.fila.usecase;

import com.fiap.techchallenge.execution.diagnostico.domain.DiagnosticoItem;
import com.fiap.techchallenge.execution.diagnostico.usecase.ItemDiagnosticoInput;
import com.fiap.techchallenge.execution.fila.domain.ItemFila;
import com.fiap.techchallenge.execution.fila.domain.StatusFila;

import java.util.List;

public interface FilaUseCase {

    ItemFila buscarPorOsId(Long osId);

    List<ItemFila> listarPorStatus(StatusFila status);

    List<DiagnosticoItem> listarDiagnostico(Long osId);

    /** OS_CRIADA: idempotente, uma OS repetida devolve o item já existente. */
    ItemFila registrarOsCriada(Long osId, String solicitacao, String veiculoPlaca, String veiculoModelo);

    ItemFila iniciarDiagnostico(Long osId, Long mecanicoId);

    ItemFila concluirDiagnostico(Long osId, List<ItemDiagnosticoInput> itens);

    /** PAGAMENTO_CONFIRMADO: reserva as peças e entra na fila; sem estoque, falha e publica EXECUCAO_FALHOU. */
    ItemFila confirmarPagamento(Long osId);

    /** Retira o próximo item da fila (prioridade e antiguidade) e inicia a execução. */
    ItemFila iniciarProximaExecucao(Long mecanicoId);

    ItemFila finalizarExecucao(Long osId);

    /** Falha informada durante a execução; compensa liberando as peças reservadas. */
    ItemFila falharExecucao(Long osId, String motivo, String detalhe);

    /** OS_CANCELADA / PAGAMENTO_ESTORNADO: idempotente; libera reservas e encerra o item. */
    void cancelar(Long osId);
}