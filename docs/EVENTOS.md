# Contrato de Eventos (SQS e SNS)

Contrato dos eventos que o **execution-service** consome e publica. Nenhum serviço lê o banco do outro: tudo que um precisa do outro vem nestes eventos. O que está aqui é o que o código implementa e os testes cobrem.

## Envelope (todas as mensagens)
```json
{
  "eventId": "7f1c3e0a-5b52-4c0e-9f43-2a7d1c9b8e10",
  "type": "OS_CRIADA",
  "version": 1,
  "occurredAt": "2026-10-22T14:00:00Z",
  "correlationId": "10",
  "payload": {
    "osId": 10,
    "criadaEm": "2026-11-08T20:41:06.266238836Z",
    "clienteId": 1,
    "veiculoId": 1,
    ...
  }
}
```
- `eventId`: UUID único. O consumidor grava em `processed_event` e ignora repetidos (SQS entrega *at-least-once*).
- `type`: tipo do evento (no root, não aninhado). Usado para roteamento: listener delega para `processarDoOsExecution()` ou `processarDoBillingExecution()`, cada método roteia via `switch` sobre `type`.
- `version`: incrementar quando o payload quebrar compatibilidade (hoje `1`).
- `occurredAt`: instante em que o evento foi gerado (UTC).
- `correlationId`: sempre o `osId` (como string). Usado nos logs para rastrear o fluxo.
- `payload.osId`: obrigatório. Identifica a OS em todo o fluxo.
- `payload.criadaEm`, `payload.clienteId`, `payload.veiculoId`: campos do contexto de billing (opcionais para o execution, copiados no `ItemFila` para auditoria).
- Filas SQS padrão (sem ordem garantida), cada uma com DLQ após 5 tentativas.

### O que o execution exige das mensagens que recebe
Para ser aceita, a mensagem precisa ter **`eventId`** (UUID válido) e **`payload.osId`**. Os demais campos do envelope são opcionais para o execution. Mensagem inválida lança erro, é reentregue e, após 5 tentativas, vai para a DLQ. Campos extras no payload são ignorados.

## Eventos consumidos pelo execution-service

| Evento | Publicado por | Fila SQS |
|---|---|---|
| `OS_CRIADA` | os-service | `os_execution` |
| `OS_CANCELADA` | os-service | `os_execution` |
| `PAGAMENTO_CONFIRMADO` | billing-service | `billing_execution` |
| `PAGAMENTO_FALHOU` | billing-service | `billing_execution` |
| `PAGAMENTO_ESTORNADO` | billing-service | `billing_execution` |

O execution consome 2 filas SQS: `os_execution` (os-service) e `billing_execution` (billing-service). O roteamento pelos 5 tipos de eventos ocorre dentro do UseCase (`EventoEntradaInteractor`), via `switch` no campo `type` do envelope. Quem as alimenta (envio direto ou fan-out por SNS) é indiferente, desde que o corpo da mensagem seja o envelope. Se a fila for assinante de um tópico SNS, a assinatura deve usar `RawMessageDelivery=true`; o execution não desembrulha o JSON do SNS (`Message`).

### OS_CRIADA
```json
{
  "eventId": "7f1c3e0a-5b52-4c0e-9f43-2a7d1c9b8e10",
  "type": "OS_CRIADA",
  "version": 1,
  "occurredAt": "2026-10-22T14:00:00Z",
  "correlationId": "10",
  "payload": {
    "osId": 10,
    "criadaEm": "2026-11-08T20:41:06.266238836Z",
    "clienteId": 1,
    "veiculoId": 1,
    "solicitacao": "Barulho no freio dianteiro",
    "veiculo": { "placa": "ABC1D23", "modelo": "Gol" }
  }
}
```
Ação: cria o item da fila (`AGUARDANDO_DIAGNOSTICO`) com solicitação, placa e modelo. Os campos `criadaEm`, `clienteId` e `veiculoId` são copiados para o `ItemFila` (contexto de billing). `solicitacao` e `veiculo` são opcionais (ficam vazios se ausentes). Reentrega da mesma OS não duplica o item.

### PAGAMENTO_CONFIRMADO
```json
{
  "eventId": "a1b2c3d4-e5f6-47g8-h9i0-j1k2l3m4n5o6",
  "type": "PAGAMENTO_CONFIRMADO",
  "version": 1,
  "occurredAt": "2026-10-22T14:15:00Z",
  "correlationId": "10",
  "payload": {
    "osId": 10,
    "orcamentoId": 55,
    "pagamentoId": "mp-123456",
    "valor": 850.00
  }
}
```
Ação: reserva as peças do diagnóstico, cria os serviços da OS e leva o item para `NA_FILA` (publica `ESTOQUE_RESERVADO`). Se faltar estoque, nada é reservado, o item vai para `FALHOU` e publica `EXECUCAO_FALHOU` (`SEM_ESTOQUE`). Só `osId` é usado.

### OS_CANCELADA
```json
{
  "eventId": "b2c3d4e5-f6g7-48h9-i0j1-k2l3m4n5o6p7",
  "type": "OS_CANCELADA",
  "version": 1,
  "occurredAt": "2026-10-22T14:20:00Z",
  "correlationId": "10",
  "payload": {
    "osId": 10,
    "motivo": "ORCAMENTO_REJEITADO"
  }
}
```
Ação: libera as reservas, cancela os serviços pendentes e marca o item como `CANCELADA`. Só `osId` é usado.

### PAGAMENTO_ESTORNADO
```json
{
  "eventId": "c3d4e5f6-g7h8-49i0-j1k2-l3m4n5o6p7q8",
  "type": "PAGAMENTO_ESTORNADO",
  "version": 1,
  "occurredAt": "2026-10-22T14:25:00Z",
  "correlationId": "10",
  "payload": {
    "osId": 10,
    "pagamentoId": "mp-123456"
  }
}
```
Ação: igual a `OS_CANCELADA`. Só `osId` é usado.

### Ordem e repetição de mensagens
- **Repetida:** o mesmo `eventId` é ignorado. Cancelar um item já `CANCELADA` ou `FALHOU` também não faz nada.
- **Cancelamento antes do `OS_CRIADA`:** é descartado (não há o que compensar).
- **`PAGAMENTO_CONFIRMADO` antes do `OS_CRIADA` ou antes de o diagnóstico ser concluído:** falha e o SQS reentrega. Se a condição não for atendida em 5 tentativas, a mensagem vai para a DLQ.
- **Cancelamento de OS já `FINALIZADA`:** é recusado (erro) e acaba na DLQ, pois uma execução concluída não pode ser desfeita.

## Eventos publicados pelo execution-service (tópico SNS `execution-events`)

Todos os eventos são publicados em um único tópico SNS dentro do **mesmo envelope** usado na entrada (com `eventId`, `type`, `version`, `occurredAt`, `correlationId`). O `type` também é enviado como atributo de mensagem para filter policies. Cada serviço interessado assina o tópico com a sua própria fila SQS (fan-out), então os consumidores não disputam mensagens. Para receber só o que importa, use uma filter policy no atributo de mensagem `type`. Se a assinatura usar `RawMessageDelivery`, o corpo da fila é o envelope; sem ela, o envelope vem dentro do campo `Message` do JSON do SNS. São gravados na outbox na mesma transação da mudança de estado e publicados em seguida, então só existem se a mudança também existiu.

### DIAGNOSTICO_CONCLUIDO → billing-service
```json
{
  "eventId": "<uuid>",
  "type": "DIAGNOSTICO_CONCLUIDO",
  "version": 1,
  "occurredAt": "2026-10-22T14:30:00Z",
  "correlationId": "10",
  "payload": {
    "osId": 10,
    "itens": [
      { "tipo": "PECA",    "refId": 7,  "quantidade": 2 },
      { "tipo": "SERVICO", "refId": 3,  "quantidade": 1 }
    ]
  }
}
```
O billing monta o orçamento só com o payload (o preço é do billing). `refId` de peça = id em `peca_estoque`; `refId` de serviço = `tipoServicoId` do catálogo do billing.

### ESTOQUE_RESERVADO → os-service (informativo)
```json
{
  "eventId": "<uuid>",
  "type": "ESTOQUE_RESERVADO",
  "version": 1,
  "occurredAt": "2026-10-22T14:35:00Z",
  "correlationId": "10",
  "payload": { "osId": 10 }
}
```

### EXECUCAO_INICIADA → os-service
```json
{
  "eventId": "<uuid>",
  "type": "EXECUCAO_INICIADA",
  "version": 1,
  "occurredAt": "2026-10-22T15:00:00Z",
  "correlationId": "10",
  "payload": {
    "osId": 10,
    "mecanicoId": 2,
    "iniciouEm": "2026-10-22T15:00:00Z"
  }
}
```
O os-service muda a OS para `EM_EXECUCAO`.

### EXECUCAO_FINALIZADA → os-service
```json
{
  "eventId": "<uuid>",
  "type": "EXECUCAO_FINALIZADA",
  "version": 1,
  "occurredAt": "2026-10-22T17:00:00Z",
  "correlationId": "10",
  "payload": {
    "osId": 10,
    "finalizouEm": "2026-10-22T17:00:00Z"
  }
}
```
O os-service muda a OS para `FINALIZADA`.

### EXECUCAO_FALHOU → os-service, billing-service
```json
{
  "eventId": "<uuid>",
  "type": "EXECUCAO_FALHOU",
  "version": 1,
  "occurredAt": "2026-10-22T15:30:00Z",
  "correlationId": "10",
  "payload": {
    "osId": 10,
    "motivo": "SEM_ESTOQUE",
    "detalhe": "Peça 7: requisitado 2, disponível 0"
  }
}
```
`motivo` é `SEM_ESTOQUE` (falta de peça ao confirmar o pagamento) ou o informado pelo mecânico em `POST /api/execucao/{osId}/falha` (`FALHA_NA_EXECUCAO` quando vazio). O billing estorna o pagamento (`PAGAMENTO_ESTORNADO`) e o os-service cancela a OS. O execution não publica evento ao compensar por `OS_CANCELADA` ou `PAGAMENTO_ESTORNADO`.

## Fluxo resumido
```
OS_CRIADA ─► [execution: diagnóstico] ─► DIAGNOSTICO_CONCLUIDO ─► [billing: orçamento/aprovação/pagamento]
PAGAMENTO_CONFIRMADO ─► [execution: reserva + NA_FILA] ─► EXECUCAO_INICIADA ─► EXECUCAO_FINALIZADA
Falha: EXECUCAO_FALHOU ─► PAGAMENTO_ESTORNADO / OS_CANCELADA
```