package com.fiap.techchallenge.execution.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

/** PostgreSQL + LocalStack (SQS e SNS) com as filas e o tópico do serviço, iguais às criadas por localstack/init-sqs.sh. */
public abstract class SqsContainerSupport extends PostgresContainerSupport {

    public static final String[] FILAS_DE_ENTRADA = {"billing_execution", "os_execution"};
    public static final String TOPICO_DE_SAIDA = "execution-events";
    /** Fila assinante do topico, usada pelos testes para ler o que o servico publica. */
    public static final String FILA_DE_SAIDA = "execution-events-test";

    protected static final LocalStackContainer LOCALSTACK =
            new LocalStackContainer(DockerImageName.parse("localstack/localstack:3")).withServices("sqs", "sns");

    static {
        LOCALSTACK.start();
        try {
            // Cria as filas de entrada
            for (String fila : FILAS_DE_ENTRADA) {
                LOCALSTACK.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", fila);
            }
            // Cria o tópico e a fila de saída
            criarTopicoComAssinante();
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao criar filas no LocalStack", e);
        }
    }

    private static void criarTopicoComAssinante() throws Exception {
        LOCALSTACK.execInContainer("awslocal", "sns", "create-topic", "--name", TOPICO_DE_SAIDA);
        LOCALSTACK.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", FILA_DE_SAIDA);
        LOCALSTACK.execInContainer("awslocal", "sns", "subscribe", "--topic-arn",
                "arn:aws:sns:us-east-1:000000000000:" + TOPICO_DE_SAIDA, "--protocol", "sqs",
                "--notification-endpoint", "arn:aws:sqs:us-east-1:000000000000:" + FILA_DE_SAIDA,
                "--attributes", "RawMessageDelivery=true");
    }

    @DynamicPropertySource
    static void propriedadesSqs(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.aws.sqs.endpoint", () -> LOCALSTACK.getEndpoint().toString());
        registry.add("spring.cloud.aws.sns.endpoint", () -> LOCALSTACK.getEndpoint().toString());
        registry.add("spring.cloud.aws.credentials.access-key", LOCALSTACK::getAccessKey);
        registry.add("spring.cloud.aws.credentials.secret-key", LOCALSTACK::getSecretKey);
        registry.add("spring.cloud.aws.region.static", LOCALSTACK::getRegion);
        registry.add("execution.sqs.billing-execution", () -> "billing_execution");
        registry.add("execution.sqs.os-execution", () -> "os_execution");
        registry.add("execution.sns.topico-arn", () -> "arn:aws:sns:us-east-1:000000000000:" + TOPICO_DE_SAIDA);
    }
}