package com.fiap.techchallenge.execution.bdd;

import com.fiap.techchallenge.execution.support.SqsContainerSupport;
import com.fiap.techchallenge.execution.support.TokenSupport;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.Message;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class ExecucaoSteps {

    /** Eventos já lidos da fila assinante do topico execution-events (a fila é compartilhada entre cenários; cada um usa OS próprias). */
    private static final List<JsonNode> EVENTOS_PUBLICADOS = new CopyOnWriteArrayList<>();

    @LocalServerPort int porta;
    @Autowired JsonMapper mapper;
    @Autowired SqsAsyncClient sqs;
    @Autowired JdbcTemplate jdbc;

    private final HttpClient http = HttpClient.newHttpClient();
    private final Map<String, Long> pecas = new HashMap<>();

    @Before
    public void limparBanco() {
        jdbc.execute("TRUNCATE execution.fila_execucao, execution.diagnostico_item, execution.reserva_peca, "
                + "execution.peca_estoque, execution.servico_execucao, execution.outbox_event, "
                + "execution.processed_event RESTART IDENTITY CASCADE");
    }

    @Given("que existe a peça {string} com {int} unidades em estoque")
    public void existePeca(String nome, int quantidade) throws Exception {
        HttpResponse<String> resposta = chamar("POST", "/api/peca", "GERENTE",
                "{\"nome\":\"%s\",\"quantidade\":%d}".formatted(nome, quantidade));
        assertThat(resposta.statusCode()).isEqualTo(201);
        pecas.put(nome, mapper.readTree(resposta.body()).path("id").asLong());
    }

    @When("o evento OS_CRIADA chega para a OS {int} com a solicitação {string} e o veículo {string} modelo {string}")
    public void osCriada(int osId, String solicitacao, String placa, String modelo) {
        enviarEvento("os_execution", "OS_CRIADA",
                "{\"osId\":%d,\"solicitacao\":\"%s\",\"veiculo\":{\"placa\":\"%s\",\"modelo\":\"%s\"}}"
                        .formatted(osId, solicitacao, placa, modelo));
        aguardarStatus(osId, "AGUARDANDO_DIAGNOSTICO");
    }

    @When("o mecânico {int} conclui o diagnóstico da OS {int} com {int} unidades da peça {string} e o serviço {int}")
    public void concluirDiagnostico(int mecanicoId, int osId, int quantidade, String peca, int servicoId) throws Exception {
        HttpResponse<String> iniciar = chamar("POST", "/api/execucao/%d/diagnostico/iniciar".formatted(osId), "MECANICO",
                "{\"mecanicoId\":%d}".formatted(mecanicoId));
        assertThat(iniciar.statusCode()).isEqualTo(200);

        String itens = "{\"itens\":[{\"tipo\":\"PECA\",\"refId\":%d,\"quantidade\":%d},{\"tipo\":\"SERVICO\",\"refId\":%d,\"quantidade\":1}]}"
                .formatted(pecas.get(peca), quantidade, servicoId);
        HttpResponse<String> concluir = chamar("POST", "/api/execucao/%d/diagnostico/concluir".formatted(osId), "MECANICO", itens);
        assertThat(concluir.statusCode()).isEqualTo(200);
    }

    @When("o evento PAGAMENTO_CONFIRMADO chega para a OS {int}")
    public void pagamentoConfirmado(int osId) {
        enviarEvento("billing_execution", "PAGAMENTO_CONFIRMADO",
                "{\"osId\":%d,\"orcamentoId\":1,\"pagamentoId\":\"mp-%d\",\"valor\":850.0}".formatted(osId, osId));
    }

    @When("o evento OS_CANCELADA chega para a OS {int}")
    public void osCancelada(int osId) {
        enviarEvento("os_execution", "OS_CANCELADA", "{\"osId\":%d,\"motivo\":\"ORCAMENTO_REJEITADO\"}".formatted(osId));
    }

    @When("o mecânico {int} inicia a próxima execução")
    public void iniciarProxima(int mecanicoId) throws Exception {
        HttpResponse<String> resposta = chamar("POST", "/api/execucao/iniciar", "MECANICO", "{\"mecanicoId\":%d}".formatted(mecanicoId));
        assertThat(resposta.statusCode()).isEqualTo(200);
    }

    @When("o mecânico {int} finaliza a execução da OS {int}")
    public void finalizar(int mecanicoId, int osId) throws Exception {
        HttpResponse<String> resposta = chamar("POST", "/api/execucao/%d/finalizar".formatted(osId), "MECANICO",
                "{\"mecanicoId\":%d}".formatted(mecanicoId));
        assertThat(resposta.statusCode()).isEqualTo(200);
    }

    @Then("a OS {int} fica {string}")
    public void osFica(int osId, String status) {
        aguardarStatus(osId, status);
    }

    @Then("o estoque da peça {string} é {int}")
    public void estoqueDaPeca(String nome, int esperado) {
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            HttpResponse<String> resposta = chamar("GET", "/api/peca/" + pecas.get(nome), "GERENTE", null);
            assertThat(mapper.readTree(resposta.body()).path("quantidade").asInt()).isEqualTo(esperado);
        });
    }

    @Then("o evento {string} é publicado para a OS {int}")
    public void eventoPublicado(String tipo, int osId) {
        aguardarEvento(tipo, osId);
    }

    @Then("o evento {string} é publicado para a OS {int} com o motivo {string}")
    public void eventoPublicadoComMotivo(String tipo, int osId, String motivo) {
        JsonNode evento = aguardarEvento(tipo, osId);
        assertThat(evento.path("payload").path("motivo").asString()).isEqualTo(motivo);
    }

    private JsonNode aguardarEvento(String tipo, int osId) {
        JsonNode[] encontrado = new JsonNode[1];
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            lerEventosPublicados();
            encontrado[0] = EVENTOS_PUBLICADOS.stream()
                    .filter(e -> tipo.equals(e.path("type").asString()) && e.path("payload").path("osId").asInt() == osId)
                    .findFirst().orElse(null);
            assertThat(encontrado[0]).as("evento %s da OS %d em execution-events", tipo, osId).isNotNull();
        });
        return encontrado[0];
    }

    private void lerEventosPublicados() {
        String url = sqs.getQueueUrl(r -> r.queueName(SqsContainerSupport.FILA_DE_SAIDA)).join().queueUrl();
        List<Message> mensagens;
        do {
            mensagens = sqs.receiveMessage(r -> r.queueUrl(url).maxNumberOfMessages(10).waitTimeSeconds(1)).join().messages();
            for (Message mensagem : mensagens) {
                EVENTOS_PUBLICADOS.add(mapper.readTree(mensagem.body()));
                sqs.deleteMessage(r -> r.queueUrl(url).receiptHandle(mensagem.receiptHandle())).join();
            }
        } while (!mensagens.isEmpty());
    }

    private void enviarEvento(String fila, String tipo, String payload) {
        String url = sqs.getQueueUrl(r -> r.queueName(fila)).join().queueUrl();
        String corpo = "{\"eventId\":\"%s\",\"type\":\"%s\",\"version\":1,\"occurredAt\":\"2026-10-22T14:00:00Z\",\"correlationId\":\"x\",\"payload\":%s}"
                .formatted(UUID.randomUUID(), tipo, payload);
        sqs.sendMessage(r -> r.queueUrl(url).messageBody(corpo)).join();
    }

    private void aguardarStatus(int osId, String esperado) {
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            HttpResponse<String> resposta = chamar("GET", "/api/execucao/" + osId, "GERENTE", null);
            assertThat(resposta.statusCode()).isEqualTo(200);
            assertThat(mapper.readTree(resposta.body()).path("status").asString()).isEqualTo(esperado);
        });
    }

    private HttpResponse<String> chamar(String metodo, String caminho, String role, String corpo) throws Exception {
        HttpRequest.Builder requisicao = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + caminho))
                .header("Authorization", TokenSupport.bearer(role))
                .header("Content-Type", "application/json");
        requisicao.method(metodo, corpo == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(corpo));
        return http.send(requisicao.build(), HttpResponse.BodyHandlers.ofString());
    }
}