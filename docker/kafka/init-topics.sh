#!/bin/bash
# Cria os tópicos Kafka usados pelo fluxo do Outbox Pattern.
#
# Roda em container separado (kafka-init) que sobe depois do broker estar
# healthy e antes da app subir — evita depender de auto-criação implícita de
# tópico (comportamento inconsistente entre client/versão) e evita comando
# manual na hora da apresentação (ver CLAUDE.md, "Zero bootstrap manual").
#
# "proposta-events.DLT" já é criado aqui desde o MVP 0, mesmo só sendo usado
# a partir do MVP 4 (Dead Letter Topic) — assim a infra de tópicos não muda
# mais depois de pronta, só o código que os usa.
set -e

BOOTSTRAP="kafka:29092"

create_topic() {
  local topic="$1"
  /opt/kafka/bin/kafka-topics.sh --bootstrap-server "$BOOTSTRAP" \
    --create --if-not-exists --topic "$topic" \
    --partitions 3 --replication-factor 1
}

echo "[kafka-init] Aguardando broker responder..."
until /opt/kafka/bin/kafka-broker-api-versions.sh --bootstrap-server "$BOOTSTRAP" >/dev/null 2>&1; do
  sleep 2
done

create_topic "proposta-events"
create_topic "proposta-events.DLT"

echo "[kafka-init] Tópicos provisionados:"
/opt/kafka/bin/kafka-topics.sh --bootstrap-server "$BOOTSTRAP" --list
