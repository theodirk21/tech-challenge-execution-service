package com.fiap.techchallenge.execution.mensageria;

import com.fiap.techchallenge.execution.diagnostico.domain.TipoItemDiagnostico;
import com.fiap.techchallenge.execution.diagnostico.usecase.ItemDiagnosticoInput;
import com.fiap.techchallenge.execution.estoque.domain.PecaEstoque;
import com.fiap.techchallenge.execution.estoque.usecase.EstoqueUseCase;
import com.fiap.techchallenge.execution.fila.domain.StatusFila;
import com.fiap.techchallenge.execution.fila.usecase.FilaUseCase;
import com.fiap.techchallenge.execution.support.PostgresContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.Message;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/** Fluxo real entre PostgreSQL e SQS (LocalStack): listeners, idempotência, outbox e publisher. */
@SpringBootTest(properties = "execution.outbox.intervalo-ms=200")
class MensageriaSqsIT extends PostgresContainerSupport {

    static final String[] ENTRADA = {"execution-os-criada", "execution-pagamento-confirmado",
            "execution-os-cancelada", "execution-pagamento-estornado"};
    static final String SAIDA = "execution-events";

    static final LocalStackContainer LOCALSTACK =
            new LocalStackContainer(DockerImageName.parse("localstack/localstack:3")).withServices("sqs");

    static {
        LOCALSTACK.start();
        try {
            for (String fila : ENTRADA) {
                LOCALSTACK.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", fila);
            }
            LOCALSTACK.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", SAIDA);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao criar filas no LocalStack", e);
        }
    }

    @DynamicPropertySource
    static void propriedades(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.aws.sqs.endpoint", () -> LOCALSTACK.getEndpoint().toString());
        registry.add("spring.cloud.aws.credentials.access-key", LOCALSTACK::getAccessKey);
        registry.add("spring.cloud.aws.credentials.secret-key", LOCALSTACK::getSecretKey);
        registry.add("spring.cloud.aws.region.static", LOCALSTACK::getRegion);
    }

    @Autowired SqsAsyncClient sqs;
    @Autowired JsonMapper mapper;
    @Autowired FilaUseCase fila;
    @Autowired EstoqueUseCase estoque;

    private final List<JsonNode> saida = new CopyOnWriteArrayList<>();

    private String url(String nomeDaFila) {
        return sqs.getQueueUrl(r -> r.queueName(nomeDaFila)).join().queueUrl();
    }

    private void enviar(String nomeDaFila, String tipo, UUID eventId, String payload) {
        String corpo = "{\"eventId\":\"%s\",\"type\":\"%s\",\"version\":1,\"occurredAt\":\"2026-10-22T14:00:00Z\",\"correlationId\":\"x\",\"payload\":%s}"
                .formatted(eventId, tipo, payload);
        sqs.sendMessage(r -> r.queueUrl(url(nomeDaFila)).messageBody(corpo)).join();
    }

    private void aguardarFilaVazia(String nomeDaFila) {
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            var atributos = sqs.getQueueAttributes(r -> r.queueUrl(url(nomeDaFila)).attributeNamesWithStrings("All"))
                    .join().attributesAsStrings();
            assertThat(atributos.get("ApproximateNumberOfMessages")).isEqualTo("0");
            assertThat(atributos.get("ApproximateNumberOfMessagesNotVisible")).isEqualTo("0");
        });
    }

    private void aguardarStatus(long osId, StatusFila esperado) {
        await().atMost(Duration.ofSeconds(30)).ignoreExceptions()
                .untilAsserted(() -> assertThat(fila.buscarPorOsId(osId).getStatus()).isEqualTo(esperado));
    }

    private JsonNode aguardarSaida(long osId, String tipo) {
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            String url = url(SAIDA);
            for (Message m : sqs.receiveMessage(r -> r.queueUrl(url).maxNumberOfMessages(10).waitTimeSeconds(1)).join().messages()) {
                saida.add(mapper.readTree(m.body()));
                sqs.deleteMessage(r -> r.queueUrl(url).receiptHandle(m.receiptHandle())).join();
            }
            assertThat(saida).anyMatch(e -> e.path("type").asString().equals(tipo)
                    && e.path("correlationId").asString().equals(String.valueOf(osId)));
        });
        return saida.stream().filter(e -> e.path("type").asString().equals(tipo)
                && e.path("correlationId").asString().equals(String.valueOf(osId))).findFirst().orElseThrow();
    }

    @Test
    void osCriadaCriaItemNaFilaComDadosDoVeiculo() {
        enviar(ENTRADA[0], "OS_CRIADA", UUID.randomUUID(),
                "{\"osId\":1001,\"solicitacao\":\"Barulho no freio\",\"veiculo\":{\"placa\":\"ABC1D23\",\"modelo\":\"Gol\",\"ano\":2018}}");

        aguardarStatus(1001L, StatusFila.AGUARDANDO_DIAGNOSTICO);
        var item = fila.buscarPorOsId(1001L);
        assertThat(item.getSolicitacao()).isEqualTo("Barulho no freio");
        assertThat(item.getVeiculoPlaca()).isEqualTo("ABC1D23");
        assertThat(item.getVeiculoModelo()).isEqualTo("Gol");
    }

    @Test
    void fluxoFelizPagamentoDuplicadoECancelamentoCompensando() {
        PecaEstoque peca = estoque.criarPeca("Filtro de óleo", 5);
        enviar(ENTRADA[0], "OS_CRIADA", UUID.randomUUID(), "{\"osId\":1002,\"solicitacao\":\"Revisão\",\"veiculo\":{\"placa\":\"XYZ9K88\",\"modelo\":\"Onix\"}}");
        aguardarStatus(1002L, StatusFila.AGUARDANDO_DIAGNOSTICO);

        fila.iniciarDiagnostico(1002L, 2L);
        fila.concluirDiagnostico(1002L, List.of(
                new ItemDiagnosticoInput(TipoItemDiagnostico.PECA, peca.getId(), 2),
                new ItemDiagnosticoInput(TipoItemDiagnostico.SERVICO, 3L, 1)));

        JsonNode diagnostico = aguardarSaida(1002L, "DIAGNOSTICO_CONCLUIDO");
        assertThat(diagnostico.path("version").asInt()).isEqualTo(1);
        assertThat(UUID.fromString(diagnostico.path("eventId").asString())).isNotNull();
        assertThat(diagnostico.path("payload").path("itens")).hasSize(2);
        assertThat(diagnostico.path("payload").path("itens").get(0).path("refId").asLong()).isEqualTo(peca.getId());

        UUID pagamento = UUID.randomUUID();
        enviar(ENTRADA[1], "PAGAMENTO_CONFIRMADO", pagamento, "{\"osId\":1002,\"pagamentoId\":\"mp-1\"}");
        enviar(ENTRADA[1], "PAGAMENTO_CONFIRMADO", pagamento, "{\"osId\":1002,\"pagamentoId\":\"mp-1\"}");
        aguardarStatus(1002L, StatusFila.NA_FILA);
        aguardarFilaVazia(ENTRADA[1]);
        assertThat(estoque.buscarPeca(peca.getId()).getQuantidade()).isEqualTo(3);
        aguardarSaida(1002L, "ESTOQUE_RESERVADO");

        enviar(ENTRADA[2], "OS_CANCELADA", UUID.randomUUID(), "{\"osId\":1002,\"motivo\":\"CLIENTE_DESISTIU\"}");
        aguardarStatus(1002L, StatusFila.CANCELADA);
        assertThat(estoque.buscarPeca(peca.getId()).getQuantidade()).isEqualTo(5);

        enviar(ENTRADA[3], "PAGAMENTO_ESTORNADO", UUID.randomUUID(), "{\"osId\":1002,\"pagamentoId\":\"mp-1\"}");
        aguardarFilaVazia(ENTRADA[3]);
        assertThat(fila.buscarPorOsId(1002L).getStatus()).isEqualTo(StatusFila.CANCELADA);
        assertThat(estoque.buscarPeca(peca.getId()).getQuantidade()).isEqualTo(5);
    }

    @Test
    void semEstoqueFalhaEPublicaExecucaoFalhou() {
        PecaEstoque peca = estoque.criarPeca("Pastilha", 1);
        enviar(ENTRADA[0], "OS_CRIADA", UUID.randomUUID(), "{\"osId\":1003,\"solicitacao\":\"Freio\",\"veiculo\":{\"placa\":\"AAA1A11\",\"modelo\":\"Uno\"}}");
        aguardarStatus(1003L, StatusFila.AGUARDANDO_DIAGNOSTICO);
        fila.iniciarDiagnostico(1003L, 2L);
        fila.concluirDiagnostico(1003L, List.of(new ItemDiagnosticoInput(TipoItemDiagnostico.PECA, peca.getId(), 2)));

        enviar(ENTRADA[1], "PAGAMENTO_CONFIRMADO", UUID.randomUUID(), "{\"osId\":1003,\"pagamentoId\":\"mp-2\"}");

        aguardarStatus(1003L, StatusFila.FALHOU);
        JsonNode falha = aguardarSaida(1003L, "EXECUCAO_FALHOU");
        assertThat(falha.path("payload").path("motivo").asString()).isEqualTo("SEM_ESTOQUE");
        assertThat(falha.path("payload").path("detalhe").asString()).contains("requisitado 2, disponível 1");
        assertThat(estoque.buscarPeca(peca.getId()).getQuantidade()).isEqualTo(1);
    }
}