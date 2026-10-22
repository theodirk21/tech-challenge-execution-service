# tech-challenge-execution-service

Serviço de **execução** do **Tech Challenge — Fase 4**.

Um dos três microsserviços (`os-service`, `billing-service` e `execution-service`) em que o monolito da oficina mecânica foi dividido. Cuida da fila de execução das ordens de serviço, do diagnóstico, da reserva de peças em estoque e dos serviços executados. Escrito em **Spring Boot (Java 21)**, executado como workload no **Amazon EKS**, com autenticação stateless via JWT, banco **PostgreSQL** próprio (Amazon RDS) e comunicação assíncrona por **Amazon SQS**.

---

## Tecnologias

- Java 21 (Amazon Corretto)
- Spring Boot 4.0.5 (Spring MVC, Spring Data JPA, Spring Security, Spring Actuator)
- PostgreSQL 16 / Flyway (1 migration, schema `execution`)
- Amazon SQS — Spring Cloud AWS 4.0.2 (LocalStack em desenvolvimento)
- JWT — JJWT (`io.jsonwebtoken:jjwt`), assinatura HMAC-SHA256
- SpringDoc OpenAPI 3 (Swagger UI)
- Testcontainers (PostgreSQL e LocalStack), JaCoCo e SonarQube
- Docker — multi-stage build (`linux/amd64`)
- Kubernetes / Amazon EKS
- GitHub Actions (CI/CD)

---

## Estrutura do Repositório

```
.github/workflows/
├── pr.yml          CI: testes, cobertura JaCoCo e build check da imagem Docker
└── deploy.yml      CD: testes, ECR, filas SQS e deploy no EKS via kubectl
app/
├── src/main/java/com/fiap/techchallenge/execution/
│   ├── fila/              fila de execução (ItemFila) e casos de uso da saga
│   ├── diagnostico/       itens do diagnóstico (peças e serviços)
│   ├── estoque/           estoque de peças e reservas
│   ├── servico/           serviços executados na OS
│   ├── mensageria/        listeners SQS, outbox, publisher e idempotência
│   ├── security/          filtro JWT e configuração Spring Security
│   ├── config/ exception/ observabilidade/
│   └── ExecutionServiceApplication.java
├── src/main/resources/
│   ├── application.properties       configuração e variáveis de ambiente
│   ├── application-prod.properties  perfil de produção (SQS real, sem LocalStack)
│   ├── logback-spring.xml           logs JSON em produção, colorido em dev
│   └── db/migration/                migration Flyway (V1)
├── src/test/                        testes unitários, de integração e ponta a ponta
├── build.gradle                     dependências, JaCoCo (mínimo 80%) e SonarQube
└── Dockerfile                       multi-stage build para linux/amd64
k8s/
├── configmap.yaml                variáveis não-sensíveis de configuração
├── deployment.yaml               Deployment (2 réplicas, RollingUpdate, probes, non-root)
├── service.yaml                  Service LoadBalancer — AWS NLB, porta 80 → 8080
├── hpa.yaml                      HPA: 2 a 10 réplicas (CPU 70%, memória 80%)
└── pod-disruption-budget.yaml    PDB: minAvailable 1
localstack/init-sqs.sh            cria as filas SQS (com DLQ) no LocalStack
docker-compose.yml                stack local: App + PostgreSQL 16 + LocalStack (SQS) + SonarQube
EVENTOS.md                        contrato dos eventos trocados entre os serviços
```

Cada módulo de negócio segue a Clean Architecture do monolito: `domain` (regras puras) → `usecase` (interactors e gateways) → `adapter` (controllers e gateways) → `framework` (JPA).

---

## Arquitetura

```
   os-service ──OS_CRIADA / OS_CANCELADA──┐          ┌──DIAGNOSTICO_CONCLUIDO──► billing-service
                                          ▼          │  EXECUCAO_FALHOU
   billing-service ──PAGAMENTO_CONFIRMADO─┤  Amazon SQS (filas execution-*)
                      PAGAMENTO_ESTORNADO ▼          │  EXECUCAO_INICIADA / FINALIZADA
                                  ┌───────────────────────────────────┐   ESTOQUE_RESERVADO
                                  │   Amazon EKS (ns: tech-challenge) │──► os-service
                                  │                                   │
                                  │   Pods Spring Boot                │
                                  │   - Listeners SQS (idempotentes)  │
                                  │   - Valida JWT (emitido no Lambda)│
                                  │   - Regras da fila e do estoque   │
                                  │   - Outbox + publisher            │
                                  │   - Migrations Flyway             │
                                  └──────────────────┬────────────────┘
                                                     │
                                  ┌──────────────────▼────────────────┐
                                  │  Amazon RDS PostgreSQL 16         │
                                  │  schema: execution                │
                                  └───────────────────────────────────┘
```

### Saga (coreografada)

Não há orquestrador: cada serviço reage aos eventos dos outros. O `osId` é o `correlationId` de todo o fluxo.

1. `OS_CRIADA` → o item entra com status `AGUARDANDO_DIAGNOSTICO`.
2. O mecânico faz o diagnóstico via REST → `DIAGNOSTICO_CONCLUIDO` e status `AGUARDANDO_PAGAMENTO`.
3. `PAGAMENTO_CONFIRMADO` → as peças são reservadas (`ESTOQUE_RESERVADO`) e o item vai para `NA_FILA`.
4. O mecânico inicia a próxima execução (FIFO) → `EXECUCAO_INICIADA`, e finaliza → `EXECUCAO_FINALIZADA`.

**Compensações**
- Estoque insuficiente → `FALHOU`, nenhuma reserva parcial e evento `EXECUCAO_FALHOU` (`SEM_ESTOQUE`).
- `OS_CANCELADA` ou `PAGAMENTO_ESTORNADO` → reservas liberadas e item `CANCELADA` (idempotente; item `FINALIZADA` não pode ser cancelado).

### Mensageria

- **Entrada:** 4 filas, cada uma com DLQ (`maxReceiveCount=5`): `execution-os-criada`, `execution-pagamento-confirmado`, `execution-os-cancelada`, `execution-pagamento-estornado`. O `eventId` é gravado em `processed_event` na mesma transação, então reentregas são ignoradas.
- **Saída:** os eventos são gravados na tabela `outbox_event` na mesma transação da mudança de estado e publicados na fila `execution-events` por um scheduler (`FOR UPDATE SKIP LOCKED`, seguro com várias réplicas). Isso garante que um evento só existe se a mudança também existiu.
- O contrato completo (envelope e payloads) está em [`EVENTOS.md`](EVENTOS.md).

### Dados entre serviços

O serviço tem banco próprio e não lê o banco de nenhum outro. Só guarda os ids e as cópias mínimas que chegam nos eventos (solicitação e placa/modelo do veículo).

---

## Pré-requisitos

- Java JDK 21
- Docker
- kubectl e AWS CLI v2 — necessários apenas para deploy manual

Para desenvolvimento local, somente Docker é necessário.

---

## Execução Local

### Docker Compose

Sobe o PostgreSQL 16, o LocalStack (SQS com as filas criadas) e a aplicação compilada via `Dockerfile`:

```bash
docker compose up --build -d
```

A API fica disponível em `http://localhost:8080` (PostgreSQL em `localhost:5434`, SQS em `localhost:4566`).

Para incluir o SonarQube na stack (porta 9000):

```bash
docker compose --profile tools up -d
```

O compose é **somente para desenvolvimento local**. Em produção não há compose: a imagem roda no EKS pelos manifestos de `k8s/`, com o perfil `prod`, que usa o SQS e o RDS reais.

### Gradle

Sobe apenas o banco e o SQS e executa a aplicação com hot-reload:

```bash
docker compose up -d db localstack
./gradlew bootRun
```

---

## Testes e Cobertura

Os testes de integração usam Testcontainers (PostgreSQL e LocalStack), então o Docker precisa estar rodando.

```bash
./gradlew test jacocoTestReport jacocoTestCoverageVerification
```

Relatório HTML gerado em `app/build/reports/jacoco/test/html/index.html`. A build falha se a cobertura ficar abaixo de 80%.

---

## Build da Imagem Docker

```bash
docker build --platform linux/amd64 -t tech-challenge-execution-service:local -f app/Dockerfile .
```

O flag `--platform linux/amd64` é obrigatório. Os nós do cluster EKS utilizam `AL2023_x86_64_STANDARD`; uma imagem ARM64 causará `ImagePullBackOff` no deploy.

---

## Variáveis de Ambiente

| Variável | Padrão (dev local) | Descrição |
|---|---|---|
| `DB_URL` (`SPRING_DATASOURCE_URL`) | `jdbc:postgresql://localhost:5434/execution` | URL JDBC de conexão com o banco |
| `DB_USER` (`SPRING_DATASOURCE_USERNAME`) | `postgres` | Usuário do banco |
| `DB_PASSWORD` (`SPRING_DATASOURCE_PASSWORD`) | `admin` | Senha do banco |
| `JWT_SECRET` | `minha-chave-secreta-dev-minimo-32-chars!!` | Chave de assinatura dos tokens JWT (a mesma usada pelo Lambda de autenticação) |
| `SPRING_PROFILES_ACTIVE` | — | `prod` em produção: remove o endpoint do LocalStack e as credenciais fixas |
| `SQS_ENDPOINT` | `http://localhost:4566` | Endpoint do SQS (somente local) |
| `AWS_REGION` | `us-east-1` | Região da AWS |
| `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` / `AWS_SESSION_TOKEN` | `test` / `test` / — | Credenciais AWS (em produção, via Secret do Kubernetes) |
| `SQS_OS_CRIADA`, `SQS_PAGAMENTO_CONFIRMADO`, `SQS_OS_CANCELADA`, `SQS_PAGAMENTO_ESTORNADO` | `execution-<evento>` | Nomes das filas consumidas |
| `SQS_SAIDA` | `execution-events` | Fila onde os eventos são publicados |
| `SQS_MAX_CONCURRENT` | `2` | Concorrência dos listeners (deve caber no pool de 3 conexões do banco) |
| `OUTBOX_ENABLED` / `OUTBOX_INTERVALO_MS` / `OUTBOX_LOTE` | `true` / `1000` / `50` | Publicação da outbox |

Em produção, o banco e o JWT são injetados pelo Kubernetes Secret `execution-secrets`, criado automaticamente pela pipeline `deploy.yml`.

---

## CI/CD (GitHub Actions)

| Workflow | Disparo | O que faz |
|---|---|---|
| `pr.yml` | Pull Request para `main` | Executa testes, verifica cobertura JaCoCo e valida o build da imagem Docker |
| `deploy.yml` | Push na `main` ou disparo manual | Roda testes e cobertura; garante o repositório ECR, as filas SQS (com DLQ) e o namespace; publica a imagem; monta o secret do banco a partir do AWS Secrets Manager; aplica os manifestos no EKS |

### Secrets e variáveis do repositório

| Nome | Tipo | Descrição |
|---|---|---|
| `AWS_ACCESS_KEY_ID` | Secret | Credencial AWS |
| `AWS_SECRET_ACCESS_KEY` | Secret | Credencial AWS |
| `AWS_SESSION_TOKEN` | Secret | Token de sessão temporário (AWS Academy Learner Lab) |
| `JWT_SECRET` | Secret | Chave de assinatura dos tokens JWT (mínimo 32 caracteres; igual à do Lambda) |
| `DB_SECRET_ID` | Variável | Nome ou ARN do secret do RDS no AWS Secrets Manager (JSON com `username`, `password`, `host`, `port` e `dbname`) |

O usuário AWS da pipeline precisa de permissão para `secretsmanager:GetSecretValue`, criação de repositório ECR e criação de filas SQS.

### Banco de dados (RDS)

O RDS é provisionado em um repositório de infraestrutura separado, e a pipeline apenas lê as credenciais dele no Secrets Manager. O banco do serviço é isolado: nenhum outro serviço o acessa. O Flyway cria o schema `execution` e as tabelas na subida.

---

## Documentação da API

Com a aplicação em execução:

- Swagger UI: `http://<HOST>/swagger-ui.html`
- OpenAPI JSON: `http://<HOST>/v3/api-docs`
- Health: `http://<HOST>/actuator/health`
- Métricas Prometheus: `http://<HOST>/actuator/prometheus`

### Endpoints

A autenticação usa o token JWT emitido pelo Lambda de autenticação (`Authorization: Bearer <token>`, com os claims `sub` e `role`). Sem token a API responde `403`.

| Método | Rota | Perfis | Descrição |
|---|---|---|---|
| GET | `/api/execucao/fila` | MECANICO, ATENDENTE, GERENTE | Lista a fila (`?status=`) |
| GET | `/api/execucao/{osId}` | MECANICO, ATENDENTE, GERENTE | Consulta o item da OS |
| GET | `/api/execucao/{osId}/diagnostico` | MECANICO, ATENDENTE, GERENTE | Itens do diagnóstico |
| GET | `/api/execucao/{osId}/servicos` | MECANICO, ATENDENTE, GERENTE | Serviços da OS |
| POST | `/api/execucao/{osId}/diagnostico/iniciar` | MECANICO, GERENTE | Inicia o diagnóstico |
| POST | `/api/execucao/{osId}/diagnostico/concluir` | MECANICO, GERENTE | Conclui o diagnóstico |
| POST | `/api/execucao/iniciar` | MECANICO, GERENTE | Inicia a próxima execução da fila |
| POST | `/api/execucao/{osId}/finalizar` | MECANICO, GERENTE | Finaliza a execução |
| POST | `/api/execucao/{osId}/falha` | MECANICO, GERENTE | Registra falha na execução |
| GET / POST | `/api/peca`, `/api/peca/{id}` | GERENTE | Consulta e cadastra peças em estoque |
| POST | `/api/peca/{id}/reposicao` | GERENTE | Repõe o estoque |

---

## Exemplos de Uso da API

### 1. Cadastro de peça em estoque

* **Requisição (`POST /api/peca`):**
  ```http
  POST /api/peca
  Content-Type: application/json
  Authorization: Bearer <token>

  {
    "nome": "Pastilha de freio",
    "quantidade": 5
  }
  ```

* **Resposta (`201 Created`):**
  ```json
  { "id": 1, "nome": "Pastilha de freio", "quantidade": 5 }
  ```

### 2. Conclusão do diagnóstico

Chega o evento `OS_CRIADA` e o mecânico inicia e conclui o diagnóstico.

* **Requisição (`POST /api/execucao/10/diagnostico/concluir`):**
  ```http
  POST /api/execucao/10/diagnostico/concluir
  Content-Type: application/json
  Authorization: Bearer <token>

  {
    "itens": [
      { "tipo": "PECA", "refId": 1, "quantidade": 2 },
      { "tipo": "SERVICO", "refId": 3, "quantidade": 1 }
    ]
  }
  ```

* **Resposta (`200 OK`):**
  ```json
  {
    "osId": 10,
    "status": "AGUARDANDO_PAGAMENTO",
    "mecanicoId": 7,
    "solicitacao": "Barulho no freio",
    "veiculoPlaca": "ABC1D23",
    "veiculoModelo": "Gol"
  }
  ```

### 3. Início e finalização da execução

Depois do evento `PAGAMENTO_CONFIRMADO` o item está em `NA_FILA` e o estoque foi reservado.

* **Iniciar a próxima da fila (`POST /api/execucao/iniciar`):**
  ```json
  { "mecanicoId": 7 }
  ```
  Resposta `200 OK` com `"status": "EM_EXECUCAO"`.

* **Finalizar (`POST /api/execucao/10/finalizar`):**
  ```json
  { "mecanicoId": 7 }
  ```
  Resposta `200 OK` com `"status": "FINALIZADA"` e o evento `EXECUCAO_FINALIZADA` publicado em `execution-events`.

---

## Análise de Código e Segurança

### SonarQube

```bash
docker compose --profile tools up -d sonarqube
# Acesse http://localhost:9000, gere um token e execute:
./gradlew test jacocoTestReport sonar -Dsonar.token=<TOKEN>
```