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
  "payload": { }
}
```
- `eventId`: UUID único. O consumidor grava em `processed_event` e ignora repetidos (SQS entrega *at-least-once*).
- `correlationId`: sempre o `osId` (como string). Usado nos logs para rastrear o fluxo.
- `version`: incrementar quando o payload quebrar compatibilidade (hoje `1`).
- `occurredAt`: instante em que o evento foi gerado (UTC).
- Filas SQS padrão (sem ordem garantida), cada uma com DLQ após 5 tentativas.

### O que o execution exige das mensagens que recebe
Para ser aceita, a mensagem precisa ter **`eventId`** (UUID válido) e **`payload.osId`**. Os demais campos do envelope são opcionais para o execution. Mensagem inválida lança erro, é reentregue e, após 5 tentativas, vai para a DLQ. Campos extras no payload são ignorados.

## Eventos consumidos pelo execution-service

| Evento | Publicado por | Fila SQS |
|---|---|---|
| `OS_CRIADA` | os-service | `execution-os-criada` |
| `PAGAMENTO_CONFIRMADO` | billing-service | `execution-pagamento-confirmado` |
| `OS_CANCELADA` | os-service | `execution-os-cancelada` |
| `PAGAMENTO_ESTORNADO` | billing-service | `execution-pagamento-estornado` |

O execution só lê essas quatro filas. Quem as alimenta (envio direto ou fan-out por SNS) é indiferente, desde que o corpo da mensagem seja o envelope. Se a fila for assinante de um tópico SNS, a assinatura deve usar `RawMessageDelivery=true`; o execution não desembrulha o JSON do SNS (`Message`).

### OS_CRIADA
```json
{ "osId": 10, "solicitacao": "Barulho no freio dianteiro",
  "veiculo": { "placa": "ABC1D23", "modelo": "Gol" } }
```
Ação: cria o item da fila (`AGUARDANDO_DIAGNOSTICO`) com solicitação, placa e modelo. `solicitacao` e `veiculo` são opcionais (ficam vazios se ausentes). Reentrega da mesma OS não duplica o item.

### PAGAMENTO_CONFIRMADO
```json
{ "osId": 10, "orcamentoId": 55, "pagamentoId": "mp-123456", "valor": 850.00 }
```
Ação: reserva as peças do diagnóstico, cria os serviços da OS e leva o item para `NA_FILA` (publica `ESTOQUE_RESERVADO`). Se faltar estoque, nada é reservado, o item vai para `FALHOU` e publica `EXECUCAO_FALHOU` (`SEM_ESTOQUE`). Só `osId` é usado.

### OS_CANCELADA
```json
{ "osId": 10, "motivo": "ORCAMENTO_REJEITADO" }
```
Ação: libera as reservas, cancela os serviços pendentes e marca o item como `CANCELADA`. Só `osId` é usado.

### PAGAMENTO_ESTORNADO
```json
{ "osId": 10, "pagamentoId": "mp-123456" }
```
Ação: igual a `OS_CANCELADA`. Só `osId` é usado.

### Ordem e repetição de mensagens
- **Repetida:** o mesmo `eventId` é ignorado. Cancelar um item já `CANCELADA` ou `FALHOU` também não faz nada.
- **Cancelamento antes do `OS_CRIADA`:** é descartado (não há o que compensar).
- **`PAGAMENTO_CONFIRMADO` antes do `OS_CRIADA` ou antes de o diagnóstico ser concluído:** falha e o SQS reentrega. Se a condição não for atendida em 5 tentativas, a mensagem vai para a DLQ.
- **Cancelamento de OS já `FINALIZADA`:** é recusado (erro) e acaba na DLQ, pois uma execução concluída não pode ser desfeita.

## Eventos publicados pelo execution-service (tópico SNS `execution-events`)

Todos os eventos são publicados em um único tópico SNS. Cada serviço interessado assina o tópico com a sua própria fila SQS (fan-out), então os consumidores não disputam mensagens. Para receber só o que importa, use uma filter policy no atributo de mensagem `type` (também presente no envelope). Se a assinatura usar `RawMessageDelivery`, o corpo da fila é o envelope; sem ela, o envelope vem dentro do campo `Message` do JSON do SNS. São gravados na outbox na mesma transação da mudança de estado e publicados em seguida, então só existem se a mudança também existiu.

### DIAGNOSTICO_CONCLUIDO → billing-service
```json
{ "osId": 10,
  "itens": [
    { "tipo": "PECA",    "refId": 7,  "quantidade": 2 },
    { "tipo": "SERVICO", "refId": 3,  "quantidade": 1 } ] }
```
O billing monta o orçamento só com isso (o preço é do billing). `refId` de peça = id em `peca_estoque`; `refId` de serviço = `tipoServicoId` do catálogo do billing.

### ESTOQUE_RESERVADO → os-service (informativo)
```json
{ "osId": 10 }
```

### EXECUCAO_INICIADA → os-service
```json
{ "osId": 10, "mecanicoId": 2, "iniciouEm": "2026-10-22T15:00:00Z" }
```
O os-service muda a OS para `EM_EXECUCAO`.

### EXECUCAO_FINALIZADA → os-service
```json
{ "osId": 10, "finalizouEm": "2026-10-22T17:00:00Z" }
```
O os-service muda a OS para `FINALIZADA`.

### EXECUCAO_FALHOU → os-service, billing-service
```json
{ "osId": 10, "motivo": "SEM_ESTOQUE", "detalhe": "Peça 7: requisitado 2, disponível 0" }
```
`motivo` é `SEM_ESTOQUE` (falta de peça ao confirmar o pagamento) ou o informado pelo mecânico em `POST /api/execucao/{osId}/falha` (`FALHA_NA_EXECUCAO` quando vazio). O billing estorna o pagamento (`PAGAMENTO_ESTORNADO`) e o os-service cancela a OS. O execution não publica evento ao cancelar por `OS_CANCELADA` ou `PAGAMENTO_ESTORNADO`.

## Fluxo resumido
```
OS_CRIADA ─► [execution: diagnóstico] ─► DIAGNOSTICO_CONCLUIDO ─► [billing: orçamento/aprovação/pagamento]
PAGAMENTO_CONFIRMADO ─► [execution: reserva + NA_FILA] ─► EXECUCAO_INICIADA ─► EXECUCAO_FINALIZADA
Falha: EXECUCAO_FALHOU ─► PAGAMENTO_ESTORNADO / OS_CANCELADA
```