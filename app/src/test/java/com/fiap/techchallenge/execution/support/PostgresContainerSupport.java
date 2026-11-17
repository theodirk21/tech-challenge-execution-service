package com.fiap.techchallenge.execution.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Um único Postgres para toda a suíte: evita reiniciar o container (e trocar a porta) com o cache de contextos do Spring. */
public abstract class PostgresContainerSupport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @ServiceConnection
    protected static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");

    static {
        POSTGRES.start();
    }



    @BeforeEach
    void limpaSeedData() {
        jdbcTemplate.update("DELETE FROM execution.fila_execucao WHERE os_id BETWEEN 1001 AND 1005");
    }
}