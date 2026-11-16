-- V3__seed_data_test.sql
-- Seed de dados para testes dos endpoints

-- 1. INSERIR PEÇAS NO ESTOQUE
INSERT INTO execution.peca_estoque (nome, quantidade) VALUES
                                                          ('Óleo do Motor 5L', 50),
                                                          ('Filtro de Ar', 100),
                                                          ('Pneu Aro 17', 30),
                                                          ('Pastilha de Freio', 80),
                                                          ('Vela de Ignição', 120),
                                                          ('Bateria 60Ah', 15)
    ON CONFLICT DO NOTHING;

-- 2. INSERIR ORDENS DE SERVIÇO NA FILA
INSERT INTO execution.fila_execucao (os_id, status, prioridade, mecanico_id, solicitacao, veiculo_placa, veiculo_modelo, entrou_em, iniciou_em) VALUES
                                                                                                                                                    (1001, 'AGUARDANDO_DIAGNOSTICO', 0, NULL, 'Troca de óleo', 'ABC-1234', 'Honda Civic 2020', NOW(), NULL),
                                                                                                                                                    (1002, 'AGUARDANDO_DIAGNOSTICO', 1, NULL, 'Revisão completa', 'XYZ-5678', 'Toyota Corolla 2019', NOW() - INTERVAL '5 minutes', NULL),
                                                                                                                                                    (1003, 'NA_FILA', 0, 10, 'Troca de pneus', 'DEF-9999', 'Ford Focus 2021', NOW() - INTERVAL '15 minutes', NULL),
                                                                                                                                                    (1004, 'EM_DIAGNOSTICO', 2, 11, 'Conserto de freios', 'GHI-1111', 'Volkswagen Gol 2018', NOW() - INTERVAL '20 minutes', NULL),
                                                                                                                                                    (1005, 'EM_EXECUCAO', 0, 12, 'Alinhamento de rodas', 'JKL-2222', 'Fiat Uno 2017', NOW() - INTERVAL '30 minutes', NOW() - INTERVAL '10 minutes')
    ON CONFLICT DO NOTHING;

-- 3. INSERIR ITENS DE DIAGNÓSTICO
INSERT INTO execution.diagnostico_item (os_id, tipo, ref_id, quantidade) VALUES
                                                                             (1001, 'PECA', 1, 1),
                                                                             (1001, 'SERVICO', 101, 1),
                                                                             (1002, 'PECA', 2, 1),
                                                                             (1002, 'PECA', 1, 1),
                                                                             (1002, 'SERVICO', 101, 1),
                                                                             (1002, 'SERVICO', 102, 1),
                                                                             (1003, 'PECA', 3, 4),
                                                                             (1003, 'SERVICO', 103, 1),
                                                                             (1004, 'PECA', 4, 2),
                                                                             (1004, 'SERVICO', 104, 1),
                                                                             (1005, 'PECA', 3, 2),
                                                                             (1005, 'SERVICO', 105, 1)
    ON CONFLICT DO NOTHING;

-- 4. INSERIR SERVIÇOS DE EXECUÇÃO
INSERT INTO execution.servico_execucao (os_id, tipo_servico_id, status) VALUES
                                                                            (1001, 101, 'AGUARDANDO_INICIO'),
                                                                            (1002, 101, 'AGUARDANDO_INICIO'),
                                                                            (1002, 102, 'AGUARDANDO_INICIO'),
                                                                            (1003, 103, 'AGUARDANDO_INICIO'),
                                                                            (1004, 104, 'AGUARDANDO_INICIO'),
                                                                            (1005, 105, 'EM_EXECUCAO')
    ON CONFLICT DO NOTHING;