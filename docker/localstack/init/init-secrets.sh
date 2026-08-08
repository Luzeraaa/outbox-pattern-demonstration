#!/bin/bash
# Init hook executado automaticamente pelo próprio LocalStack
# (/etc/localstack/init/ready.d/) assim que o serviço fica pronto — não
# precisa de container extra nem comando manual (ver CLAUDE.md, "Zero
# bootstrap manual").
#
# Provisiona o segredo no Secrets Manager que será consumido pela app a
# partir do MVP 5. Criado desde o MVP 0 para a infra de LocalStack já ficar
# estável (só o código que lê o segredo muda depois).
set -e

awslocal secretsmanager create-secret \
  --name outbox-demo/credentials \
  --description "Credenciais fictícias da POC do Outbox Pattern" \
  --secret-string '{"usuario":"demo","senha":"demo123"}' \
  --region us-east-1 \
  || echo "[localstack-init] Secret 'outbox-demo/credentials' já existe, seguindo."

echo "[localstack-init] Recursos AWS provisionados."
