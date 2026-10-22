CREATE SCHEMA IF NOT EXISTS execution;

CREATE TABLE execution.fila_execucao (
    id            BIGSERIAL PRIMARY KEY,
    os_id         BIGINT      NOT NULL UNIQUE,
    status        VARCHAR(30) NOT NULL,
    prioridade    INT         NOT NULL DEFAULT 0,
    mecanico_id   BIGINT,
    entrou_em     TIMESTAMP   NOT NULL DEFAULT now(),
    iniciou_em    TIMESTAMP,
    finalizou_em  TIMESTAMP,
    solicitacao     TEXT,
    veiculo_placa   VARCHAR(10),
    veiculo_modelo  VARCHAR(80),
    CONSTRAINT ck_fila_status CHECK (status IN (
        'AGUARDANDO_DIAGNOSTICO', 'EM_DIAGNOSTICO', 'AGUARDANDO_PAGAMENTO',
        'NA_FILA', 'EM_EXECUCAO', 'FINALIZADA', 'FALHOU', 'CANCELADA'))
);
CREATE INDEX idx_fila_status_prioridade
    ON execution.fila_execucao (status, prioridade DESC, entrou_em);

CREATE TABLE execution.diagnostico_item (
    id          BIGSERIAL PRIMARY KEY,
    os_id       BIGINT      NOT NULL,
    tipo        VARCHAR(10) NOT NULL CHECK (tipo IN ('PECA', 'SERVICO')),
    ref_id      BIGINT      NOT NULL,
    quantidade  INT         NOT NULL DEFAULT 1 CHECK (quantidade > 0)
);
CREATE INDEX idx_diagnostico_os ON execution.diagnostico_item (os_id);

CREATE TABLE execution.peca_estoque (
    id          BIGSERIAL PRIMARY KEY,
    nome        VARCHAR(120) NOT NULL,
    quantidade  INT          NOT NULL CHECK (quantidade >= 0)
);

CREATE TABLE execution.reserva_peca (
    id          BIGSERIAL PRIMARY KEY,
    os_id       BIGINT      NOT NULL,
    peca_id     BIGINT      NOT NULL REFERENCES execution.peca_estoque (id),
    quantidade  INT         NOT NULL CHECK (quantidade > 0),
    status      VARCHAR(15) NOT NULL CHECK (status IN ('RESERVADA', 'LIBERADA', 'BAIXADA'))
);
CREATE INDEX idx_reserva_os ON execution.reserva_peca (os_id);

CREATE TABLE execution.outbox_event (
    id            UUID        PRIMARY KEY,
    aggregate_id  BIGINT      NOT NULL,
    type          VARCHAR(60) NOT NULL,
    payload       JSONB       NOT NULL,
    created_at    TIMESTAMP   NOT NULL DEFAULT now(),
    published_at  TIMESTAMP
);
CREATE INDEX idx_outbox_pendentes ON execution.outbox_event (created_at) WHERE published_at IS NULL;

CREATE TABLE execution.processed_event (
    event_id      UUID      PRIMARY KEY,
    processed_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE execution.servico_execucao (
    id               BIGSERIAL PRIMARY KEY,
    os_id            BIGINT      NOT NULL,
    tipo_servico_id  BIGINT      NOT NULL,
    status           VARCHAR(20) NOT NULL
        CHECK (status IN ('AGUARDANDO_INICIO', 'EM_EXECUCAO', 'FINALIZADO', 'DELETADO')),
    data_inicio      TIMESTAMP,
    data_fim         TIMESTAMP
);
CREATE INDEX idx_servico_execucao_os ON execution.servico_execucao (os_id);
