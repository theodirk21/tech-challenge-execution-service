#!/bin/bash
# Filas consumidas pelo execution-service (cada uma com DLQ)
for q in execution-os-criada execution-pagamento-confirmado execution-os-cancelada execution-pagamento-estornado; do
  awslocal sqs create-queue --queue-name "${q}-dlq"
  awslocal sqs create-queue --queue-name "$q" \
    --attributes '{"RedrivePolicy":"{\"deadLetterTargetArn\":\"arn:aws:sqs:us-east-1:000000000000:'"${q}"'-dlq\",\"maxReceiveCount\":\"5\"}"}'
done

# Fila de saída com os eventos publicados pelo execution-service
awslocal sqs create-queue --queue-name execution-events
