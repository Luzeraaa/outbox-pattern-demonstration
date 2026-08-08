# Outbox Pattern — Demonstração

POC que demonstra o **Outbox Pattern**: um padrão de integração que garante a
publicação confiável de eventos (ex.: num tópico Kafka) mesmo diante de falhas —
sem perder mensagem e sem publicar antes da transação de negócio ser confirmada.

> Contexto completo de arquitetura, decisões técnicas e roadmap do projeto está
> em [`CLAUDE.md`](./CLAUDE.md) (documentação de execução/desenvolvimento).
> Este README é o guia para **rodar e demonstrar** o projeto.

## Como rodar

Único pré-requisito: **Docker Desktop** instalado e rodando.

```bash
docker-compose up -d
```

Isso sobe a infraestrutura inteira **e a aplicação**, com o Mongo já em
replica-set, os tópicos Kafka já criados e os recursos AWS já provisionados no
LocalStack — sem nenhum comando manual adicional. Leva cerca de 1 a 2 minutos
na primeira vez (build da imagem da aplicação); nas próximas é bem mais rápido.

Para acompanhar a subida:

```bash
docker-compose ps
```

Todos os serviços devem aparecer como `healthy` (a aplicação pode levar alguns
segundos a mais que a infra, já que depende dela estar pronta primeiro).

Para resetar o estado da demo entre apresentações (limpa os dados do Mongo):

```bash
docker-compose down -v
docker-compose up -d
```

## Serviços e portas

| Serviço | URL | Para que serve na demo |
|---|---|---|
| **Swagger UI** | http://localhost:8080/swagger-ui.html | Disparar os endpoints da aplicação clicando em "Try it out" — não precisa de Postman/curl. |
| **Actuator Health** | http://localhost:8080/actuator/health | Confirma que a aplicação e suas dependências (Mongo) estão de pé. |
| **Kafka UI** | http://localhost:8082 | Ver visualmente a mensagem chegando no tópico `proposta-events` e, em cenário de falha, na DLT `proposta-events.DLT`. |
| **Mongo Express** | http://localhost:8081 | Ver o documento da Proposta e do OutboxEvent salvos no Mongo. |
| Mongo (driver/Compass) | `mongodb://localhost:27017/outbox_demo?replicaSet=rs0` | Inspecionar dados manualmente fora da demo, se quiser (ver seção abaixo). |
| Kafka (bootstrap externo) | `localhost:9092` | Conectar um client Kafka externo à máquina, se necessário. |
| LocalStack | http://localhost:4566 | Endpoint AWS local (Secrets Manager) — uso interno da aplicação, não precisa abrir na demo. |

## Como demonstrar — criação de Proposta + Outbox Pattern

1. Abra o [Swagger UI](http://localhost:8080/swagger-ui.html) e expanda
   `POST /api/propostas`.
2. Clique em "Try it out", informe o corpo, por exemplo:
   ```json
   { "tipoAmortizacao": "SAC" }
   ```
3. Clique em "Execute" — a resposta `201 Created` traz a Proposta já com `id`
   e status `EM_ANDAMENTO`.
4. Abra o [Mongo Express](http://localhost:8081), banco `outbox_demo`:
   - coleção `propostas` → mostra o documento da Proposta criada;
   - coleção `outbox_events` → mostra o registro do Outbox Pattern, com
     `status: "ENVIADO"` (a publicação no Kafka já aconteceu no fast-path
     pós-commit, quase instantânea).
5. Abra o [Kafka UI](http://localhost:8082), tópico `proposta-events`, e
   mostre a mensagem chegando (serializada em Avro, chave de partição =
   id da Proposta).

> A transição da Proposta para `PROCESSADA` (consumo da mensagem, idempotência,
> Circuit Breaker/Retry/DLT) chega no MVP 4.

## Como demonstrar — resiliência do Scheduler (Kafka fora do ar)

O `OutboxReprocessamentoScheduler` é a rede de segurança: se a publicação
imediata (fast-path) falhar, o registro fica `outbox_events.status: PENDENTE`
e o Scheduler tenta de novo periodicamente, com backoff exponencial.

1. Pare o Kafka: `docker stop outbox-pattern-demonstration-kafka-1` (ou pelo
   Docker Desktop).
2. Crie uma Proposta pelo Swagger normalmente — a resposta ainda vem `201`
   (a criação em si não depende do Kafka), mas no Mongo Express o
   `outbox_events` correspondente fica `PENDENTE` (ou `EM_PROCESSAMENTO`
   momentaneamente, enquanto o Scheduler tenta).
3. Suba o Kafka de novo: `docker start outbox-pattern-demonstration-kafka-1`.
4. Em poucos segundos (respeitando o backoff da última tentativa), o
   Scheduler republica sozinho — o `outbox_events` vira `ENVIADO` e a
   mensagem aparece no Kafka UI, sem nenhum comando manual de reenvio.

> Se o Kafka ficar indisponível por tempo suficiente para esgotar
> `outbox.scheduler.max-tentativas` (default 5, com backoff de 5s a 5min), o
> registro vai para `FALHA_DEFINITIVA` — estado terminal, não é mais
> reprocessado automaticamente (evita loop infinito numa "poison message").
> Fica visível no Mongo Express para investigação manual.

## Conectando com um cliente MongoDB (opcional)

Para inspecionar os dados fora da demo, qualquer cliente MongoDB gratuito serve —
recomendação: [MongoDB Compass](https://www.mongodb.com/products/compass).

String de conexão:

```
mongodb://localhost:27017/outbox_demo?replicaSet=rs0
```

## Desenvolvimento (fora da demo)

A aplicação roda containerizada no `docker-compose` — não é preciso Java/IDE
para demonstrar o projeto. Para desenvolver:

- **JDK 21** configurado como Project SDK.
- **IntelliJ IDEA**: abrir a pasta pelo `build.gradle.kts`.
- Para rodar só a infra (Mongo/Kafka/LocalStack) e a aplicação direto pela IDE,
  suba os serviços de infra e aponte a aplicação para `localhost` nas portas
  acima (os hosts internos `mongo`/`kafka` só resolvem dentro da rede do
  compose).

## Status do projeto

Ver [`CLAUDE.md`](./CLAUDE.md), seção "Roadmap de MVPs resilientes", para o
que já está pronto e o que falta.
