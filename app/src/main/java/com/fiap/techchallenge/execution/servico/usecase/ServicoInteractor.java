package com.fiap.techchallenge.execution.servico.usecase;

import com.fiap.techchallenge.execution.servico.domain.Servico;
import com.fiap.techchallenge.execution.servico.domain.ServicoStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ServicoInteractor implements ServicoUseCase {

    private final ServicoGateway gateway;

    public ServicoInteractor(ServicoGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Servico> listarPorOs(Long osId) {
        return gateway.listarPorOsId(osId);
    }

    @Override
    public void criarParaOs(Long osId, List<Long> tiposServicoIds) {
        tiposServicoIds.forEach(tipo -> gateway.salvar(Servico.criar(tipo, osId)));
    }

    @Override
    public void iniciarDaOs(Long osId) {
        gateway.listarPorOsId(osId).stream()
                .filter(s -> s.getStatus() == ServicoStatus.AGUARDANDO_INICIO)
                .forEach(s -> {
                    s.iniciar();
                    gateway.salvar(s);
                });
    }

    @Override
    public void finalizarDaOs(Long osId) {
        gateway.listarPorOsId(osId).stream()
                .filter(s -> s.getStatus() == ServicoStatus.EM_EXECUCAO)
                .forEach(s -> {
                    s.finalizar();
                    gateway.salvar(s);
                });
    }

    @Override
    public void cancelarPendentesDaOs(Long osId) {
        gateway.listarPorOsId(osId).stream()
                .filter(s -> s.getStatus() == ServicoStatus.AGUARDANDO_INICIO)
                .forEach(s -> {
                    s.deletar();
                    gateway.salvar(s);
                });
    }
}