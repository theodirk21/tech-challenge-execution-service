package com.fiap.techchallenge.execution.fila.adapter.gateway;

import com.fiap.techchallenge.execution.fila.framework.persistence.entity.ItemFilaEntity;
import com.fiap.techchallenge.execution.fila.framework.persistence.repository.ItemFilaRepository;
import com.fiap.techchallenge.execution.fila.usecase.FilaGateway;
import com.fiap.techchallenge.execution.fila.domain.ItemFila;
import com.fiap.techchallenge.execution.fila.domain.StatusFila;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class FilaGatewayImpl implements FilaGateway {

    private final ItemFilaRepository jpa;

    public FilaGatewayImpl(ItemFilaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public ItemFila salvar(ItemFila item) {
        ItemFila salvo = jpa.save(ItemFilaEntity.from(item)).toDomain();
        item.setId(salvo.getId());
        return salvo;
    }

    @Override
    public Optional<ItemFila> buscarPorOsId(Long osId) {
        return jpa.findByOsId(osId).map(ItemFilaEntity::toDomain);
    }

    @Override
    public Optional<ItemFila> buscarPorOsIdComLock(Long osId) {
        return jpa.findByOsIdForUpdate(osId).map(ItemFilaEntity::toDomain);
    }

    @Override
    public Optional<ItemFila> proximoDaFilaComLock() {
        return jpa.findProximoForUpdateSkipLocked().map(ItemFilaEntity::toDomain);
    }

    @Override
    public List<ItemFila> listarPorStatus(StatusFila status) {
        return jpa.findByStatusOrderByPrioridadeDescEntrouEmAsc(status).stream()
                .map(ItemFilaEntity::toDomain).toList();
    }
}
