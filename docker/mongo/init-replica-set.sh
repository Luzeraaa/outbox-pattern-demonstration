#!/bin/bash
# Inicializa o replica-set single-node "rs0" do Mongo.
#
# Por que isso é necessário: transação multi-documento (Proposta + OutboxEvent
# na mesma transação, ver CLAUDE.md) só existe em cluster com replica-set no
# MongoDB — um mongod "standalone" não suporta sessões transacionais. Como a
# demo usa um único nó Mongo representando o DocumentDB da AWS, precisamos
# iniciar esse único nó como replica-set de um membro só.
#
# Roda em container separado (mongo-init) que sobe depois do mongo estar
# healthy e antes da app subir — ninguém digita esse comando na hora da
# apresentação (ver CLAUDE.md, "Zero bootstrap manual").
set -e

is_initiated() {
  mongosh --host mongo --quiet --eval "try { rs.status().ok } catch(e) { print(0) }" 2>/dev/null
}

STATUS=$(is_initiated)
if [ "$STATUS" = "1" ]; then
  echo "[mongo-init] Replica set rs0 já inicializado. Nada a fazer."
  exit 0
fi

echo "[mongo-init] Inicializando replica set rs0..."
mongosh --host mongo --eval "rs.initiate({_id: 'rs0', members: [{_id: 0, host: 'mongo:27017'}]})"
echo "[mongo-init] Replica set rs0 inicializado com sucesso."
