package com.fiap.techchallenge.execution.bdd;

import com.fiap.techchallenge.execution.support.SqsContainerSupport;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * A aplicação sobe inteira (PostgreSQL e SQS reais). O contexto é descartado a cada cenário para que o
 * scheduler da outbox e os listeners de um cenário não continuem ativos nos testes seguintes.
 */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "execution.outbox.intervalo-ms=200")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class CucumberSpringConfig {

    @ServiceConnection
    protected static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");

    protected static final LocalStackContainer LOCALSTACK =
            new LocalStackContainer(DockerImageName.parse("localstack/localstack:3")).withServices("sqs", "sns");

    static {
        POSTGRES.start();
        LOCALSTACK.start();
        try {
            for (String fila : SqsContainerSupport.FILAS_DE_ENTRADA) {
                LOCALSTACK.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", fila);
            }
            criarTopicoComAssinante();
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao criar filas no LocalStack", e);
        }
    }

    private static void criarTopicoComAssinante() throws Exception {
        LOCALSTACK.execInContainer("awslocal", "sns", "create-topic", "--name", SqsContainerSupport.TOPICO_DE_SAIDA);
        LOCALSTACK.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", SqsContainerSupport.FILA_DE_SAIDA);
        LOCALSTACK.execInContainer("awslocal", "sns", "subscribe", "--topic-arn",
                "arn:aws:sns:us-east-1:000000000000:" + SqsContainerSupport.TOPICO_DE_SAIDA, "--protocol", "sqs",
                "--notification-endpoint", "arn:aws:sqs:us-east-1:000000000000:" + SqsContainerSupport.FILA_DE_SAIDA,
                "--attributes", "RawMessageDelivery=true");
    }

    @DynamicPropertySource
    static void propriedadesSqs(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.aws.sqs.endpoint", () -> LOCALSTACK.getEndpoint().toString());
        registry.add("spring.cloud.aws.sns.endpoint", () -> LOCALSTACK.getEndpoint().toString());
        registry.add("spring.cloud.aws.credentials.access-key", LOCALSTACK::getAccessKey);
        registry.add("spring.cloud.aws.credentials.secret-key", LOCALSTACK::getSecretKey);
        registry.add("spring.cloud.aws.region.static", LOCALSTACK::getRegion);
    }
}