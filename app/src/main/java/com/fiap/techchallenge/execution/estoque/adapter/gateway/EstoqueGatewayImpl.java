package com.fiap.techchallenge.execution.estoque.adapter.gateway;

import com.fiap.techchallenge.execution.estoque.domain.PecaEstoque;
import com.fiap.techchallenge.execution.estoque.domain.ReservaPeca;
import com.fiap.techchallenge.execution.estoque.framework.persistence.entity.PecaEstoqueEntity;
import com.fiap.techchallenge.execution.estoque.framework.persistence.entity.ReservaPecaEntity;
import com.fiap.techchallenge.execution.estoque.framework.persistence.repository.PecaEstoqueRepository;
import com.fiap.techchallenge.execution.estoque.framework.persistence.repository.ReservaPecaRepository;
import com.fiap.techchallenge.execution.estoque.usecase.EstoqueGateway;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class EstoqueGatewayImpl implements EstoqueGateway {

    private final PecaEstoqueRepository pecas;
    private final ReservaPecaRepository reservas;

    public EstoqueGatewayImpl(PecaEstoqueRepository pecas, ReservaPecaRepository reservas) {
        this.pecas = pecas;
        this.reservas = reservas;
    }

    @Override
    public PecaEstoque salvarPeca(PecaEstoque peca) {
        PecaEstoque salva = pecas.save(PecaEstoqueEntity.from(peca)).toDomain();
        peca.setId(salva.getId());
        return salva;
    }

    @Override
    public Optional<PecaEstoque> buscarPecaPorId(Long id) {
        return pecas.findById(id).map(PecaEstoqueEntity::toDomain);
    }

    @Override
    public Optional<PecaEstoque> buscarPecaPorIdComLock(Long id) {
        return pecas.findByIdForUpdate(id).map(PecaEstoqueEntity::toDomain);
    }

    @Override
    public List<PecaEstoque> listarPecas() {
        return pecas.findAll().stream().map(PecaEstoqueEntity::toDomain).toList();
    }

    @Override
    public ReservaPeca salvarReserva(ReservaPeca reserva) {
        ReservaPeca salva = reservas.save(ReservaPecaEntity.from(reserva)).toDomain();
        reserva.setId(salva.getId());
        return salva;
    }

    @Override
    public List<ReservaPeca> listarReservasPorOsId(Long osId) {
        return reservas.findByOsIdOrderById(osId).stream().map(ReservaPecaEntity::toDomain).toList();
    }
}