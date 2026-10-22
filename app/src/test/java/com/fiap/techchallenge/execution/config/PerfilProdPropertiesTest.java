package com.fiap.techchallenge.execution.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class PerfilProdPropertiesTest {

    @Test
    void perfilProdNaoUsaEndpointNemCredenciaisFixas() throws Exception {
        Properties p = PropertiesLoaderUtils.loadAllProperties("application-prod.properties");

        assertThat(p.getProperty("spring.cloud.aws.sqs.endpoint")).isEmpty();
        assertThat(p.getProperty("spring.cloud.aws.credentials.access-key")).isEmpty();
        assertThat(p.getProperty("spring.cloud.aws.credentials.secret-key")).isEmpty();
    }

    @Test
    void perfilPadraoApontaParaLocalStack() throws Exception {
        Properties p = PropertiesLoaderUtils.loadAllProperties("application.properties");

        assertThat(p.getProperty("spring.cloud.aws.sqs.endpoint")).contains("localhost:4566");
        assertThat(p.getProperty("spring.cloud.aws.sqs.queue-not-found-strategy")).isEqualTo("FAIL");
    }
}
