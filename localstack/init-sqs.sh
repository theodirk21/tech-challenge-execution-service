#!/bin/bash
# Filas consumidas pelo execution-service
awslocal sqs create-queue --queue-name os_execution
awslocal sqs create-queue --queue-name billing_execution

# Topico SNS com os eventos publicados pelo execution-service
TOPIC_ARN=$(awslocal sns create-topic --name execution-events --query TopicArn --output text)
awslocal sqs create-queue --queue-name execution-events-local
awslocal sns subscribe --topic-arn "$TOPIC_ARN" --protocol sqs \
  --notification-endpoint arn:aws:sqs:us-east-1:000000000000:execution-events-local \
  --attributes RawMessageDelivery=true