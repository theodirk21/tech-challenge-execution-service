# Contrato de Eventos (SQS) — Proposta

Para revisão do grupo (os-service, billing-service, execution-service). Nenhum serviço lê o banco do outro: tudo que um precisa do outro vem nestes eventos.

## Envelope (todas as mensagens)
```json
{
  "eventId": "7f1c...-uuid",
  "type": "OS_CRIADA",
  "version": 1,
  "occurredAt": "2026-10-22T14:00:00Z",
  "correlationId": "10",
  "payload": { }
}
```
- `eventId`: UUID único; o consumidor guarda em `processed_event` e ignora repetidos (SQS entrega *at-least-once*).
- `correlationId`: sempre o `osId` (como string). Usado nos logs para rastrear o fluxo.
- `version`: incrementar quando o payload quebrar compatibilidade.
- Filas SQS padrão, uma por consumidor/evento, cada uma com DLQ (5 tentativas).

## Eventos consumidos pelo execution-service

| Evento | Publicado por | Fila SQS |
|---|---|---|
| `OS_CRIADA` | os-service | `execution-os-criada` |
| `PAGAMENTO_CONFIRMADO` | billing-service | `execution-pagamento-confirmado` |
| `OS_CANCELADA` | os-service | `execution-os-cancelada` |
| `PAGAMENTO_ESTORNADO` | billing-service | `execution-pagamento-estornado` |

### OS_CRIADA
```json
{ "osId": 10, "solicitacao": "Barulho no freio dianteiro",
  "veiculo": { "placa": "ABC1D23", "modelo": "Gol", "ano": 2018 } }
```
Ação: cria o item da fila (`AGUARDANDO_DIAGNOSTICO`) com solicitação, placa e modelo.

### PAGAMENTO_CONFIRMADO
```json
{ "osId": 10, "orcamentoId": 55, "pagamentoId": "mp-123456", "valor": 850.00 }
```
Ação: reserva as peças do diagnóstico; item vai para `NA_FILA`. Se faltar estoque, publica `EXECUCAO_FALHOU`.

### OS_CANCELADA
```json
{ "osId": 10, "motivo": "ORCAMENTO_REJEITADO" }
```
Ação: libera reservas e marca o item como `CANCELADA`.

### PAGAMENTO_ESTORNADO
```json
{ "osId": 10, "pagamentoId": "mp-123456" }
```
Ação: libera reservas e marca o item como `CANCELADA`.

## Eventos publicados pelo execution-service (fila `execution-events`)

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
os-service muda a OS para `EM_EXECUCAO`.

### EXECUCAO_FINALIZADA → os-service
```json
{ "osId": 10, "finalizouEm": "2026-10-22T17:00:00Z" }
```
os-service muda a OS para `FINALIZADA`.

### EXECUCAO_FALHOU → os-service, billing-service
```json
{ "osId": 10, "motivo": "SEM_ESTOQUE", "detalhe": "Peça 7: requisitado 2, disponível 0" }
```
billing estorna o pagamento (`PAGAMENTO_ESTORNADO`); os-service cancela a OS.

## Fluxo resumido
```
OS_CRIADA ─► [execution: diagnóstico] ─► DIAGNOSTICO_CONCLUIDO ─► [billing: orçamento/aprovação/pagamento]
PAGAMENTO_CONFIRMADO ─► [execution: reserva + NA_FILA] ─► EXECUCAO_INICIADA ─► EXECUCAO_FINALIZADA
Falha: EXECUCAO_FALHOU ─► PAGAMENTO_ESTORNADO / OS_CANCELADA
```

## Pontos a confirmar com o grupo
1. Campos de `veiculo` no `OS_CRIADA` (precisamos pelo menos de placa e modelo).
2. Quem cria cada fila SQS em produção (hoje o LocalStack cria `execution-*`; os-service e billing terão as suas).
3. Um tópico SNS + filas por assinante (fan-out) ou envio direto para cada fila? Hoje está desenhado como envio direto.
4. `refId` de serviço: o billing é dono do catálogo; o execution só guarda o id.
