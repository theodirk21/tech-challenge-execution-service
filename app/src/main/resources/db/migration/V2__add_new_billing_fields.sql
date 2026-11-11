-- Adiciona novos campos à tabela fila_execucao para suportar a nova arquitetura de eventos
ALTER TABLE execution.fila_execucao
    ADD COLUMN criada_em VARCHAR(50),
    ADD COLUMN cliente_id BIGINT,
    ADD COLUMN veiculo_id BIGINT,
    ADD COLUMN correlation_id VARCHAR(100);
