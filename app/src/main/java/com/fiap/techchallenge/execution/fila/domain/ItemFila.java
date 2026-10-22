package com.fiap.techchallenge.execution.fila.domain;

import com.fiap.techchallenge.execution.exception.ConflictException;

import java.time.LocalDateTime;

public class ItemFila {

    private Long id;
    private Long osId;
    private StatusFila status;
    private int prioridade;
    private Long mecanicoId;
    private String solicitacao;
    private String veiculoPlaca;
    private String veiculoModelo;
    private LocalDateTime entrouEm;
    private LocalDateTime iniciouEm;
    private LocalDateTime finalizouEm;

    public ItemFila() {}

    public ItemFila(Long id, Long osId, StatusFila status, int prioridade, Long mecanicoId,
                    String solicitacao, String veiculoPlaca, String veiculoModelo,
                    LocalDateTime entrouEm, LocalDateTime iniciouEm, LocalDateTime finalizouEm) {
        this.id = id;
        this.osId = osId;
        this.status = status;
        this.prioridade = prioridade;
        this.mecanicoId = mecanicoId;
        this.solicitacao = solicitacao;
        this.veiculoPlaca = veiculoPlaca;
        this.veiculoModelo = veiculoModelo;
        this.entrouEm = entrouEm;
        this.iniciouEm = iniciouEm;
        this.finalizouEm = finalizouEm;
    }

    public static ItemFila criar(Long osId, String solicitacao, String veiculoPlaca, String veiculoModelo,
                                 LocalDateTime agora) {
        return new ItemFila(null, osId, StatusFila.AGUARDANDO_DIAGNOSTICO, 0, null,
                solicitacao, veiculoPlaca, veiculoModelo, agora, null, null);
    }

    // Transições de status

    public void iniciarDiagnostico(Long mecanicoId) {
        exigirStatus(StatusFila.AGUARDANDO_DIAGNOSTICO, StatusFila.EM_DIAGNOSTICO);
        this.mecanicoId = mecanicoId;
        this.status = StatusFila.EM_DIAGNOSTICO;
    }

    public void concluirDiagnostico() {
        exigirStatus(StatusFila.EM_DIAGNOSTICO, StatusFila.AGUARDANDO_PAGAMENTO);
        this.status = StatusFila.AGUARDANDO_PAGAMENTO;
    }

    public void confirmarPagamento() {
        exigirStatus(StatusFila.AGUARDANDO_PAGAMENTO, StatusFila.NA_FILA);
        this.status = StatusFila.NA_FILA;
    }

    public void iniciarExecucao(Long mecanicoId, LocalDateTime agora) {
        exigirStatus(StatusFila.NA_FILA, StatusFila.EM_EXECUCAO);
        this.mecanicoId = mecanicoId;
        this.iniciouEm = agora;
        this.status = StatusFila.EM_EXECUCAO;
    }

    public void finalizar(LocalDateTime agora) {
        exigirStatus(StatusFila.EM_EXECUCAO, StatusFila.FINALIZADA);
        this.finalizouEm = agora;
        this.status = StatusFila.FINALIZADA;
    }

    /** Compensação da saga: falha depois do pagamento (ex.: sem estoque). */
    public void falhar(LocalDateTime agora) {
        if (status != StatusFila.AGUARDANDO_PAGAMENTO && status != StatusFila.NA_FILA && status != StatusFila.EM_EXECUCAO) {
            throw transicaoInvalida(StatusFila.FALHOU, "AGUARDANDO_PAGAMENTO, NA_FILA ou EM_EXECUCAO");
        }
        this.finalizouEm = agora;
        this.status = StatusFila.FALHOU;
    }

    /** OS cancelada ou pagamento estornado: encerra o item em qualquer estado não terminal. */
    public void cancelar(LocalDateTime agora) {
        if (status.isTerminal()) {
            throw new ConflictException("A OS %d já está encerrada com status '%s' e não pode ser cancelada!".formatted(osId, status));
        }
        this.finalizouEm = agora;
        this.status = StatusFila.CANCELADA;
    }

    public boolean podeSerRetiradoDaFila() {
        return status == StatusFila.NA_FILA;
    }

    private void exigirStatus(StatusFila esperado, StatusFila destino) {
        if (status != esperado) {
            throw transicaoInvalida(destino, esperado.name());
        }
    }

    private ConflictException transicaoInvalida(StatusFila destino, String esperado) {
        return new ConflictException("Erro: A OS %d está em '%s'; para ir para '%s' deve estar em %s!"
                .formatted(osId, status, destino, esperado));
    }

    public Long getId() { return id; }
    public Long getOsId() { return osId; }
    public StatusFila getStatus() { return status; }
    public int getPrioridade() { return prioridade; }
    public Long getMecanicoId() { return mecanicoId; }
    public String getSolicitacao() { return solicitacao; }
    public String getVeiculoPlaca() { return veiculoPlaca; }
    public String getVeiculoModelo() { return veiculoModelo; }
    public LocalDateTime getEntrouEm() { return entrouEm; }
    public LocalDateTime getIniciouEm() { return iniciouEm; }
    public LocalDateTime getFinalizouEm() { return finalizouEm; }

    public void setId(Long id) { this.id = id; }
    public void setPrioridade(int prioridade) { this.prioridade = prioridade; }
}
