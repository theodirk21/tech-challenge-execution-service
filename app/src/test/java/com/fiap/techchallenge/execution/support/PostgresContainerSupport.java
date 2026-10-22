package com.fiap.techchallenge.execution.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Um único Postgres para toda a suíte: evita reiniciar o container (e trocar a porta) com o cache de contextos do Spring. */
public abstract class PostgresContainerSupport {

    @ServiceConnection
    protected static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");

    static {
        POSTGRES.start();
    }
}