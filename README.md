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
