#!/bin/bash
# Filas consumidas pelo execution-service (cada uma com DLQ)
for q in execution-os-criada execution-pagamento-confirmado execution-os-cancelada execution-pagamento-estornado; do
  awslocal sqs create-queue --queue-name "${q}-dlq"
  awslocal sqs create-queue --queue-name "$q" \
    --attributes '{"RedrivePolicy":"{\"deadLetterTargetArn\":\"arn:aws:sqs:us-east-1:000000000000:'"${q}"'-dlq\",\"maxReceiveCount\":\"5\"}"}'
done

# Topico SNS com os eventos publicados pelo execution-service.
# Os outros servicos assinam com a propria fila; a fila abaixo e so para inspecionar localmente.
TOPIC_ARN=$(awslocal sns create-topic --name execution-events --query TopicArn --output text)
awslocal sqs create-queue --queue-name execution-events-local
awslocal sns subscribe --topic-arn "$TOPIC_ARN" --protocol sqs \
  --notification-endpoint arn:aws:sqs:us-east-1:000000000000:execution-events-local \
  --attributes RawMessageDelivery=true
