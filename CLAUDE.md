# OutboxPattern_Demonstration — Contexto do Projeto

> Este arquivo é lido automaticamente pelo Claude Code ao abrir esta pasta.
> Objetivo: POC para apresentação de liderança demonstrando Outbox Pattern.
> Este arquivo é PARA O CLAUDE (contexto de execução). Documentação para humanos
> vive no README.md do repositório — ver regra de processo abaixo.

## Objetivo do projeto (não perder de vista)
POC para **apresentar a uma liderança não-técnica ou semi-técnica**. Isso pesa mais
que "seguir a lista de tecnologias à risca": qualquer decisão que exija passo manual
na hora da apresentação é um problema a ser resolvido, não um detalhe menor.
Prioridade #1: `docker-compose up -d` sobe TUDO, incluindo a aplicação, e a demo
acontece sem terminal, sem Postman, sem comando manual de infra.

## Revisão de consistência feita em 08/2026 (aplicar desde o MVP 0)
Durante o alinhamento identificamos pontos que, se não corrigidos, quebrariam a
promessa de "rodar fácil e intuitivo". Decisões corrigidas:

1. **A aplicação Spring também precisa estar no docker-compose** (Dockerfile
   multi-stage), não só a infra (Mongo/Kafka/LocalStack). Rodar via IntelliJ é só
   para desenvolvimento; a demo em si não pode depender da IDE.
2. **Healthchecks em todos os serviços** + `depends_on: condition: service_healthy`
   no compose. Sem isso a app pode subir e crashar em loop porque Mongo/Kafka/
   LocalStack ainda não terminaram de inicializar — péssimo na frente da liderança.
3. **Zero bootstrap manual**: replica-set do Mongo (`rs.initiate`), criação dos
   tópicos Kafka e provisionamento de recursos no LocalStack (Secrets Manager)
   devem rodar via scripts de init dentro do próprio compose (init container ou
   LocalStack init hooks em `/etc/localstack/init/ready.d/`). Ninguém digita
   comando de infra na hora da apresentação.
4. **Swagger/OpenAPI (springdoc-openapi) exposto** — a liderança interage clicando
   "Try it out" no navegador, sem precisar saber o que é Postman/curl.
5. **Kafka UI (provectuslabs/kafka-ui) e Mongo Express como serviços do compose** —
   permitem *mostrar visualmente* a mensagem chegando no tópico, indo para a DLT
   em caso de falha, e o documento salvo no Mongo, sem abrir terminal.
6. **Endpoint/rotina de reset opcional** para reiniciar o estado da demo entre
   apresentações repetidas (ex.: limpar propostas de teste) sem precisar
   `docker-compose down -v` toda vez.

## Perfil de trabalho (preferências do usuário — sempre aplicar)
- Especialista em Kotlin/Java: pode ser direto e técnico, sem explicações básicas.
- Respostas objetivas e claras.
- Se começar a perder contexto/coerência, PARAR e avisar o usuário.
- NUNCA fazer merge (develop → staging → main) sem aprovação explícita do usuário.
- Entrega dividida em MVPs **resilientes** (ver seção de roadmap): cada MVP deixa o
  sistema num estado que sobe sozinho com `docker-compose up -d`, mesmo incompleto.
- Não escrever testes unitários (decisão explícita do usuário para este POC).
- Commits semânticos (feat:, fix:, docs:, chore:, refactor:) em português, granulares.
- **Fluxo de branches/MVP (processo obrigatório, uma branch por MVP)**:
  `main` (protegida) ← `staging` ← `develop` ← branch de feature por MVP.
  Configurar GitHub Actions com gate manual antes de main. Para cada MVP:
  1. No início do MVP, `git pull` a branch `develop` local (garante partir do
     que já foi mergeado, incluindo MVPs anteriores).
  2. Criar uma nova branch a partir de `develop` para aquele MVP (ex.:
     `feature/mvp2-outbox-pattern`).
  3. Commitar todo o trabalho do MVP nessa branch (commits granulares, ver
     acima).
  4. Ao final do MVP, parar e aguardar o usuário revisar/abrir PR/fazer o
     merge para `develop` — **nunca abrir a branch do MVP seguinte antes
     disso**. Só seguir para o próximo MVP depois que o usuário informar
     explicitamente que o merge foi feito (não basta "ok, pode seguir"; é o
     merge em si que libera o próximo `pull` + nova branch do passo 1).
- **README.md do repositório deve ser atualizado ao final de CADA MVP** — é o que a
  liderança e qualquer pessoa nova vai ler para rodar o projeto. Nunca considerar um
  MVP "pronto" com o README desatualizado.
- **Releia este arquivo (CLAUDE.md) no início de toda sessão nova**, incluindo a
  última entrada da seção "Atualizações" no final dele, antes de agir.
- **Ao final de todo loop de trabalho** (fim da sessão, ou antes de parar para
  aguardar aprovação do usuário) — **NUNCA pule este passo** — adicione uma nova
  entrada na seção "Atualizações" ao final deste arquivo, seguindo o template lá
  descrito, e commite isso junto (`docs: atualiza registro de sessão`). É o que
  garante que a próxima sessão retome exatamente de onde essa parou, em vez de
  precisar reconstruir o contexto.

## Convenções de código
- Comentários (KDoc em classes/interfaces públicas + inline onde ajuda) devem
  explicar o **porquê / a regra de negócio**, em linguagem natural e ubíqua com o
  domínio (Proposta, Outbox, Amortização SAC/PRICE, EM_ANDAMENTO/PROCESSADA) — não
  são para narrar mecanicamente o que a linha de código já diz.
  - Ruim: `// incrementa contador` acima de `contador++`
  - Bom: `// Uma proposta só é elegível para reprocessamento após 3 tentativas
    // falhas, evitando reenvio agressivo em instabilidades curtas do Kafka.`
- Nomes de classes/variáveis em português quando representam conceito de domínio
  (Proposta, TipoAmortizacao), em inglês quando é infraestrutura/técnico padrão
  (OutboxPublisher, KafkaConfig).
- Cada classe pública tem um KDoc de topo de 2-4 linhas explicando seu papel no
  fluxo (útil para a liderança seguir o código durante a apresentação, se pedir).

## Arquitetura (Hexagonal + DDD)
Camadas isoladas por dependência — **o domínio nunca conhece infraestrutura**:

```
domain/
├── model/        Proposta (Aggregate Root), TipoAmortizacaoEnum, StatusPropostaEnum,
│                 OutboxEvent, OutboxStatusEnum — POJOs/data class puros, SEM
│                 anotação de Mongo/Spring. Nunca importam infraestrutura.
├── event/        PropostaCriadaEvent — Domain Event (fato de negócio imutável;
│                 é o payload que vira o Avro publicado no Kafka)
└── port/
    ├── input/    CriarPropostaInputPort, ProcessarPropostaInputPort — contrato
    │             que a aplicação oferece (o que o "mundo de fora" pode pedir)
    └── output/   PropostaOutputPort, OutboxOutputPort,
                  PropostaEventPublisherOutputPort — contrato que a infra
                  precisa cumprir (o que o domínio precisa do "mundo de fora")

application/
└── usecase/      CriarPropostaUsecase, ProcessarPropostaUsecase,
                  ReprocessarOutboxUsecase — implementam os *InputPort,
                  orquestram domínio + *OutputPort. É aqui que mora a transação.

infrastructure/
├── input/
│   ├── rest/     PropostaController + dto/ (PropostaRequestDto,
│   │             PropostaResponseDto) — nunca expõe o domínio direto
│   └── kafka/    PropostaListener (consumer)
├── output/
│   ├── persistence/mongo/
│   │   PropostaDocument, PropostaMongoRepository (Spring Data MongoDB),
│   │   PropostaRepositoryAdapter (implementa PropostaOutputPort),
│   │   OutboxDocument, OutboxMongoRepository, OutboxRepositoryAdapter,
│   │   mapper/ PropostaMapper, OutboxMapper (Domain ↔ Document)
│   └── messaging/kafka/
│       PropostaProducer (implementa PropostaEventPublisherOutputPort)
└── scheduler/    OutboxReprocessamentoScheduler

config/           MongoConfig, KafkaConfig, OpenApiConfig, ResilienceConfig
```

Regras da camada de domínio (não negociáveis):
- `domain/model` e `domain/event` NUNCA importam Spring/Mongo/Kafka. É Kotlin puro.
- Persistência usa um modelo espelho (`*Document`) com as anotações do Mongo; a
  conversão Domain ↔ Document é responsabilidade do `*Mapper`, nunca do domínio.
- REST nunca recebe/retorna o domínio direto — sempre via `*Dto` + `*Mapper`.
- Toda dependência de fora para dentro passa por uma porta (`*InputPort` ou
  `*OutputPort`); a aplicação nunca chama Spring Data/Kafka client diretamente,
  só através dessas interfaces.

## Convenção de nomenclatura (sufixos)
| Sufixo | Papel | Exemplo |
|---|---|---|
| `*Usecase` | Caso de uso (camada application, orquestra domínio + ports) | `CriarPropostaUsecase` |
| `*InputPort` | Porta de entrada (interface, o que a aplicação oferece) | `CriarPropostaInputPort` |
| `*OutputPort` | Porta de saída (interface, o que a infra precisa cumprir) | `PropostaOutputPort` |
| `*Adapter` | Implementação de output port na infraestrutura | `PropostaRepositoryAdapter` |
| `*Controller` | Adapter de entrada HTTP/REST | `PropostaController` |
| `*Listener` | Adapter de entrada Kafka (consumer) | `PropostaListener` |
| `*Producer` | Adapter de saída Kafka (publisher) | `PropostaProducer` |
| `*Document` | Modelo de persistência Mongo (nunca é o domínio) | `PropostaDocument` |
| `*Repository` | Interface Spring Data MongoDB (uso interno do Adapter) | `PropostaMongoRepository` |
| `*Mapper` | Conversão Domain ↔ Dto ↔ Document | `PropostaMapper` |
| `*Dto` | Entrada/saída HTTP, nunca expõe o domínio direto | `PropostaRequestDto` |
| `*Enum` | Enumeração de domínio | `TipoAmortizacaoEnum` |
| `*Event` | Domain Event (fato de negócio imutável) | `PropostaCriadaEvent` |
| `*Config` | Configuração técnica (`@Configuration`) | `KafkaConfig` |
| `*Scheduler` | Job agendado | `OutboxReprocessamentoScheduler` |
| `*Exception` | Exceção de domínio/aplicação | `PropostaNaoEncontradaException` |

## Stack confirmada
- Kotlin + Java 21, Spring Boot 4.1.x (Spring Framework 7.0.8), Gradle Kotlin DSL.
- MongoDB único (container `mongo` real, replica set single-node, init automático)
  — representa o DocumentDB da AWS (wire-compatible). Motivo: DocumentDB só é
  suportado na imagem PRO do LocalStack, incompatível com demo 100% gratuita.
- Persistência via **Spring Data MongoDB** (`MongoRepository` + `MongoTemplate`
  para a transação multi-documento do outbox) — não é JPA. JPA (Jakarta
  Persistence API) é específico para banco relacional via Hibernate; o módulo
  correto do ecossistema Spring para Mongo é Spring Data MongoDB, que segue a
  mesma filosofia de repositórios do Spring Data.
- Kafka real (KRaft, sem Zookeeper) direto no docker-compose — NÃO via MSK do
  LocalStack. Motivo: MSK não está no tier gratuito do LocalStack; a própria doc do
  LocalStack recomenda o padrão "self-managed Kafka" ao lado do LocalStack.
- LocalStack (free/Hobby) para recursos AWS gratuitos: Secrets Manager (credenciais),
  provisionado via init hook — cumpre o requisito de "criação de recursos AWS via
  LocalStack" sem depender de plano pago.
- Avro: schema em `src/main/avro/*.avsc` (padrão de mercado do plugin Gradle Avro,
  que gera as classes automaticamente em `build/generated-main-avro-java` durante o
  build), SEM Schema Registry (é demonstrativo). Arquivo versionado no próprio
  repositório — não vai para o S3.
- Resilience4j: Circuit Breaker + Retry no listener Kafka.
- DLT: DefaultErrorHandler + DeadLetterPublishingRecoverer (Spring Kafka).
- Outbox: Proposta + OutboxEvent persistidos na mesma transação Mongo. Publicação
  imediata via `@TransactionalEventListener(phase = AFTER_COMMIT)` (evento de
  aplicação em memória, sem reconsulta ao banco) — único componente que faz
  polling no outbox é o Scheduler, evitando redundância de leitura.
- Scheduler: ÚNICO ponto que faz *polling* na coleção outbox (status PENDENTE),
  usando *claim* atômico (`findAndModify`, transição PENDENTE→EM_PROCESSAMENTO com
  dono+expiração) para evitar publicação duplicada se houver mais de uma instância
  da app rodando. Backoff exponencial entre tentativas; após limite de tentativas
  configurável, o registro vai para `FALHA_DEFINITIVA` (estado terminal, não
  reprocessado automaticamente — evita retry infinito de "poison message"). Só
  existe para cobrir o que o fast-path pós-commit não conseguiu confirmar
  (crash entre commit e publish, Kafka indisponível, etc.).
- springdoc-openapi (Swagger UI), kafka-ui, mongo-express — camada de demo/observação.

## Domínio
`Proposta(id, status: StatusPropostaEnum, tipoAmortizacao: TipoAmortizacaoEnum(SAC|PRICE))`
Status: `EM_ANDAMENTO` → (evento publicado e consumido com sucesso) → `PROCESSADA`

`OutboxEvent(id, aggregateId: propostaId, eventType, payload, status: OutboxStatusEnum,
tentativas, createdAt, updatedAt)`
Status: `PENDENTE` → (`ENVIADO` | retry com backoff até esgotar tentativas →
`FALHA_DEFINITIVA`)

## Fluxo de eventos
1. POST cria Proposta (status EM_ANDAMENTO) + registro OutboxEvent (status
   PENDENTE) — mesma transação Mongo. O payload do evento (`PropostaCriadaEvent`)
   já carrega todos os dados que o consumidor precisa (id, status, tipoAmortizacao)
   — o listener nunca reconsulta a Proposta para montar o evento.
2. Fast-path: `@TransactionalEventListener(AFTER_COMMIT)` dispara publicação
   imediata no tópico Kafka `proposta-events` (Avro), usando o `propostaId` como
   **chave de partição** (garante ordenação por agregado). Sucesso → OutboxEvent
   vira `ENVIADO`.
3. Fallback: Scheduler cobre o que o fast-path não confirmou — faz *claim* atômico
   dos registros `PENDENTE`, tenta publicar de novo com backoff exponencial; após
   N tentativas vai para `FALHA_DEFINITIVA` (não trava o fluxo, mas fica visível
   para investigação).
4. Listener consome o tópico, aplica Circuit Breaker + Retry, e só então muda
   status para PROCESSADA. **Idempotente por construção**: se a Proposta já está
   PROCESSADA, a mensagem é ignorada (no-op) em vez de reprocessada — necessário
   porque o Outbox Pattern garante *at-least-once delivery*, então duplicata é
   esperada, não exceção.
5. Falha definitiva de integração (após Circuit Breaker/Retry exauridos) →
   mensagem enviada para DLT (`proposta-events.DLT`), visível no Kafka UI.
6. OutboxEvents `ENVIADO` têm TTL/limpeza periódica (índice TTL do Mongo) para a
   coleção outbox não crescer indefinidamente — prática de mercado, não é
   "arquivo de auditoria" permanente.

## Padrão Outbox — auditoria de aderência ao mercado (feita em 08/2026)
Comparei o desenho acima com as duas implementações reconhecidas hoje no mercado:

- **Polling Publisher** (o que este projeto usa): processo próprio consulta a
  tabela/coleção outbox e publica. Simples, sem infra extra, latência de
  segundos. É o ponto de partida recomendado para protótipos/POCs.
- **CDC / Transaction log tailing** (Debezium, MongoDB Change Streams): lê o log
  de transação do banco em vez de consultar a tabela; latência de milissegundos,
  zero carga extra de query, mas exige Kafka Connect + operação de um connector —
  é o padrão recomendado para produção em alta escala.

**Decisão consciente para esta POC**: manter Polling (fast-path pós-commit +
Scheduler de fallback), sem Debezium/Change Streams — infraestrutura extra não se
paga para uma demonstração, e o objetivo aqui é clareza didática do padrão, não
throughput de produção. Vale deixar isso explícito na apresentação: é uma escolha
de trade-off, não desconhecimento do padrão mais robusto.

Pontos que estavam faltando e foram corrigidos nesta revisão:
1. Confusão entre "quem faz polling" — antes parecia que o fast-path também
   consultava o banco, redundante com o Scheduler. Agora só o Scheduler consulta.
2. Faltava *claim* atômico para múltiplas instâncias não publicarem o mesmo evento
   duas vezes — adicionado (`findAndModify` com dono+expiração).
3. Faltava limite de tentativas / tratamento de "poison message" no Scheduler —
   sem isso um evento permanentemente inválido reprocessaria para sempre.
4. Faltava exigir **idempotência explícita no listener** — o Outbox Pattern só
   garante *at-least-once*, nunca *exactly-once*; sem checar o status atual antes
   de aplicar a transição, uma duplicata reprocessaria indevidamente.
5. Faltava a **chave de partição do Kafka** — sem usar `propostaId` como key, não
   há garantia de ordenação de eventos do mesmo agregado.
6. Faltava política de limpeza do outbox (TTL) — sem isso a coleção cresce sem
   limite para sempre, mesmo após o evento já ter sido entregue.

## Pré-requisitos e como usar/conectar cada ferramenta
| Ferramenta | Necessário para | Como usar/conectar |
|---|---|---|
| Docker Desktop | Rodar a demo inteira | `docker-compose up -d` na raiz do projeto. Só isso. |
| JDK 21 | Apenas desenvolvimento na IDE (não é exigido para a demo, a app roda containerizada) | Configurar como Project SDK no IntelliJ |
| IntelliJ IDEA (Community ou Ultimate) | Desenvolvimento assistido pelo Claude Code | Abrir a pasta pelo `build.gradle.kts` |
| Git | Versionamento/push | `git clone` do repo já criado no GitHub |
| Claude Code (opcional, requer Pro/Max/Team) | Desenvolvimento assistido com push real | Ver histórico de instalação já resolvido nesta conversa |
| Cliente MongoDB (Compass, opcional) | Inspecionar dados manualmente fora da demo | String de conexão será documentada no README ao final do MVP 0 |
| Kafka UI (incluso no compose) | Ver mensagens/tópicos/DLT na demo | Abrir no navegador, porta documentada no README |
| Mongo Express (incluso no compose) | Ver documentos no Mongo na demo | Abrir no navegador, porta documentada no README |
| Swagger UI (embutido na app) | Disparar os endpoints na demo | Abrir no navegador, porta documentada no README |
| AWS CLI / awslocal (opcional) | Inspecionar recursos no LocalStack via terminal | Não é necessário para a demo; só para curiosidade técnica |

> As portas exatas de cada serviço serão fixadas e documentadas no README.md assim
> que o docker-compose do MVP 0 existir — não adivinhar/hardcodar aqui.

## Entregáveis finais (últimos MVPs)
- Documentação técnica em PDF (por classe/método/usecase/domínio, linguagem ubíqua).
- Fluxograma da funcionalidade em PDF.
- README.md completo: pré-requisitos, como subir, portas de cada serviço, variáveis
  de conexão ao Mongo local, sugestão de cliente MongoDB gratuito, como demonstrar
  cada etapa do fluxo (incluindo o cenário de falha → DLT).

## Roadmap de MVPs resilientes
Cada MVP segue este template — critério de "pronto" inclui sempre "sobe sozinho
com `docker-compose up -d`", mesmo que o escopo ainda esteja incompleto.

- [x] **MVP 0** — Esqueleto + orquestração completa
  - Entrega: pacotes `domain/application/infrastructure/config` (hexagonal, vazios
    ou com stubs), Dockerfile multi-stage da app, docker-compose (Mongo com init de
    replica-set, Kafka KRaft com criação de tópico automática, LocalStack com init
    hooks, App, Kafka UI, Mongo Express), healthchecks, Swagger vazio.
  - Pronto quando: `docker-compose up -d` sobe tudo saudável e `/actuator/health`
    responde 200 pelo Swagger.
  - Demo: abrir Swagger, `docker ps` mostrando tudo healthy, Kafka UI e Mongo
    Express acessíveis.
  - Rollback: branch isolada; `docker-compose down -v` reseta sem afetar o resto.

- [x] **MVP 1** — Domínio Proposta
  - Entrega: `Proposta` (domain/model) + `TipoAmortizacaoEnum`/`StatusPropostaEnum`,
    `CriarPropostaInputPort` + `CriarPropostaUsecase`, `PropostaOutputPort` +
    `PropostaRepositoryAdapter` (Spring Data MongoDB, `PropostaDocument` +
    `PropostaMapper`), `PropostaController` + `PropostaRequestDto`/
    `PropostaResponseDto`, documentado no Swagger.
  - Pronto quando: dá para criar uma proposta pelo Swagger e ver o documento no
    Mongo Express, mesmo sem outbox ainda.
  - Demo: Swagger → POST → Mongo Express mostra o documento salvo.
  - Rollback: reverte só a branch do MVP 1; MVP 0 continua de pé.

- [x] **MVP 2** — Outbox Pattern + fast-path pós-commit + Avro
  - Entrega: `OutboxEvent`/`OutboxDocument`/`OutboxRepositoryAdapter` (transação
    Mongo junto com Proposta), `PropostaCriadaEvent` (domain event, payload
    autocontido), `PropostaProducer` publicando via
    `@TransactionalEventListener(AFTER_COMMIT)` com `propostaId` como chave de
    partição, serialização Avro, tópico `proposta-events`.
  - Pronto quando: ao criar proposta, o evento aparece no Kafka UI e o
    OutboxEvent correspondente vira `ENVIADO`.
  - Demo: criar proposta → mostrar mensagem chegando no Kafka UI em tempo real.
  - Rollback: outbox desabilitável por flag de config sem derrubar o MVP 1.

- [x] **MVP 3** — Scheduler de reprocessamento (fallback resiliente)
  - Entrega: `OutboxReprocessamentoScheduler`, *claim* atômico
    (`findAndModify`, PENDENTE→EM_PROCESSAMENTO com dono+expiração), backoff
    exponencial, limite de tentativas com transição para `FALHA_DEFINITIVA`.
  - Pronto quando: simulando falha (derrubando o container do Kafka), o scheduler
    reprocessa o outbox `PENDENTE` quando o Kafka volta; um evento forçado a
    falhar sempre vai para `FALHA_DEFINITIVA` após o limite, sem loop infinito.
  - Demo: parar Kafka → criar proposta → subir Kafka → ver scheduler publicar.
  - Rollback: scheduler é opt-in via configuração; não interfere no fluxo síncrono.

- [ ] **MVP 4** — Listener idempotente + Circuit Breaker/Retry + DLT
  - Entrega: `PropostaListener`, checagem de status atual antes de transicionar
    (idempotência — ignora se já `PROCESSADA`), Resilience4j, DLT.
  - Pronto quando: fluxo feliz completo funciona sozinho; reenviar a mesma
    mensagem manualmente não duplica efeito; falha forçada vai para a DLT,
    visível no Kafka UI.
  - Demo: fluxo feliz completo + reenvio da mesma mensagem (mostra idempotência)
    + simulação de erro indo para a DLT.
  - Rollback: listener em consumer group isolado; desabilitar não quebra a
    publicação.

- [ ] **MVP 5** — Recursos AWS via LocalStack
  - Entrega: Secrets Manager provisionado via init hook, app lendo credenciais
    de lá.
  - Pronto quando: sobe junto com o resto, sem passo manual de AWS CLI.
  - Demo: app funcionando com dados vindos do Secrets Manager (+ awslocal opcional
    para quem quiser ver por baixo dos panos).
  - Rollback: fallback para variável de ambiente local se o Secrets Manager falhar,
    evitando travar a demo.

- [ ] **MVP 6** — Documentação (PDF técnico + fluxograma) + README final
  - Entrega: PDF técnico, PDF fluxograma, README completo.
  - Pronto quando: alguém novo consegue rodar o projeto só lendo o README.
  - Demo: apresentação guiada pelo README + fluxograma.

- [ ] **MVP 7** — GitHub Actions + revisão final dos commits
  - Entrega: workflow develop→staging automático, gate manual obrigatório para main.
  - Pronto quando: pipeline verde e PR para main aguardando aprovação humana.
  - Rollback: `workflow_dispatch`/aprovação manual evita merge acidental.

**Ao iniciar uma sessão, comece pelo primeiro item não marcado seguindo o fluxo de
branches acima (`pull` de `develop` → nova branch do MVP) e pare ao final do MVP
aguardando o usuário informar que o merge daquela branch para `develop` foi feito
antes de seguir para o próximo.**

## Atualizações

> Template de cada entrada nova (mais recente sempre no topo):
> ```
> ### AAAA-MM-DD — <MVP em andamento/concluído>
> - Feito: ...
> - Estado atual: sobe com `docker-compose up -d`? o quê está healthy?
> - Decisões/gotchas técnicos relevantes para a próxima sessão
> - Próximo passo: ...
> ```

### 2026-08-08 — MVP 3 concluído, aguardando merge para iniciar MVP 4
- Feito: seguido o fluxo de branch por MVP — `checkout develop` + `pull`
  (trouxe o merge do PR do MVP 2) e criada `feature/mvp3-scheduler-reprocessamento`
  a partir dela. `OutboxStatusEnum` ganhou `EM_PROCESSAMENTO`/`FALHA_DEFINITIVA`;
  `OutboxEvent` ganhou `claimedBy`/`claimExpiraEm` (claim atômico) e
  `proximaTentativaEm` (backoff). `OutboxOutputPort` ganhou
  `reivindicarProximoPendente` (findAndModify atômico), `marcarComoEnviadoPorId`,
  `reagendarAposFalha` e `marcarComoFalhaDefinitiva`, implementados no
  `OutboxRepositoryAdapter`. `ReprocessarOutboxInputPort` +
  `ReprocessarOutboxUsecase` (novo usecase de aplicação) orquestram o claim +
  republish + decisão de backoff/limite. `OutboxReprocessamentoScheduler`
  (`@Scheduled`, opt-in via `outbox.scheduler.enabled`) é o único ponto de
  entrada/polling. README atualizado com o passo a passo de demo (derrubar
  Kafka → criar Proposta → subir Kafka → Scheduler recupera sozinho).
- Estado atual: **validado de ponta a ponta, incluindo os 3 cenários de
  resiliência** (não só o happy path):
  1. Fluxo feliz sem duplicação (ver gotcha #1 abaixo).
  2. Kafka derrubado → Proposta criada (POST ainda responde `201` em ~3s,
     não trava) → OutboxEvent fica PENDENTE com tentativas/backoff reais
     incrementando a cada falha genuína do Scheduler → Kafka religado →
     Scheduler publica sozinho em poucos segundos, OutboxEvent vira ENVIADO,
     mensagem confirmada no tópico exatamente uma vez, claim limpo.
  3. "Poison message" (eventType desconhecido inserido direto no Mongo) vai
     direto para FALHA_DEFINITIVA sem gastar tentativas de retry, e não é
     reprocessada de novo depois (estado terminal confirmado, sem loop).
  Stack sobe limpo com `docker-compose down -v` + `up -d --build`. Dados de
  teste limpos, stack parado ao final da sessão.
- **Dois bugs reais encontrados e corrigidos durante a validação (não só
  hipóteses de code review — reproduzidos rodando de verdade):**
  1. **Fast-path vs. Scheduler duplicavam publicação em quase toda criação,
     não só em falhas genuínas.** Causa: a query de claim do Scheduler
     considerava qualquer PENDENTE elegível imediatamente, então um tick do
     `@Scheduled` (a cada 5s) quase sempre alcançava o registro antes do
     fast-path terminar de publicar. Corrigido com um período de carência
     (`outbox.scheduler.grace-period-segundos`, default 6s): um PENDENTE de
     1ª tentativa só fica elegível pro Scheduler depois de "envelhecer" esse
     tanto, dando tempo do fast-path terminar. Reentativas (que já têm
     `proximaTentativaEm`) continuam elegíveis sem essa carência, porque ali
     o fast-path já comprovadamente falhou.
  2. **`marcarComoFalhaDefinitiva` não persistia o contador final de
     `tentativas`** — o registro ficava com o penúltimo valor, escondendo a
     tentativa que de fato estourou o limite. Corrigido passando `tentativas`
     como parâmetro explícito para a porta/adapter.
  3. **Descoberta adicional durante a implementação (corrigida antes de virar
     bug em produção)**: `PropostaProducer.publicar` original era
     fire-and-forget (`kafkaTemplate.send` sem aguardar o `CompletableFuture`)
     — nesse modelo, o fast-path/Scheduler nunca veriam uma falha do Kafka
     (a exceção aconteceria de forma assíncrona, descartada). Corrigido para
     bloquear no `.get(timeout)` do futuro retornado, com `MAX_BLOCK_MS_CONFIG`
     baixo no producer (3s) para não herdar o default de 60s do client e
     travar a request/rodada do Scheduler quando o Kafka está fora do ar.
- Próximo passo: **aguardar o usuário confirmar que o merge de
  `feature/mvp3-scheduler-reprocessamento` → `develop` foi feito**. Só então:
  `checkout develop` + `pull`, criar `feature/mvp4-listener-idempotente-dlt`
  (ou nome equivalente) a partir dela, e iniciar o MVP 4 (Listener idempotente
  + Circuit Breaker/Retry + DLT).

### 2026-08-08 — MVP 2 concluído, aguardando merge para iniciar MVP 3
- Feito: seguido o novo fluxo de branch por MVP — `checkout develop` + `pull`
  (trouxe o merge do PR do MVP 0+1) e criada `feature/mvp2-outbox-pattern` a
  partir dela. Implementado o núcleo do Outbox Pattern: `OutboxEvent` +
  `OutboxStatusEnum` (domain/model, só PENDENTE/ENVIADO neste MVP —
  EM_PROCESSAMENTO/FALHA_DEFINITIVA ficam para o MVP 3, quando o Scheduler
  de fato existir), `PropostaCriadaEvent` (domain/event, payload autocontido:
  propostaId/status/tipoAmortizacao/criadaEm), `OutboxOutputPort` +
  `PropostaEventPublisherOutputPort` (portas de saída), `OutboxDocument` +
  `OutboxMongoRepository` + `OutboxMapper` + `OutboxRepositoryAdapter`
  (persistência Mongo, `marcarComoEnviado` via `MongoTemplate` direto — update
  pontual, não recarrega o documento), schema Avro em
  `src/main/avro/proposta_criada_event.avsc` (`PropostaCriadaEventAvro`, sem
  Schema Registry), `PropostaProducer` (implementa
  `PropostaEventPublisherOutputPort`, serializa Avro binário manualmente e
  publica em `proposta-events` usando `propostaId` como chave de partição),
  `KafkaConfig` (ProducerFactory `<String, ByteArray>`) e `MongoConfig`
  (`MongoTransactionManager`, necessário para o `@Transactional` funcionar).
  `CriarPropostaUsecase` passou a gravar Proposta + OutboxEvent na mesma
  transação Mongo e a expor `aoConfirmarCriacao` (`@TransactionalEventListener`,
  `AFTER_COMMIT`) como fast-path: publica no Kafka só depois do commit e, se
  publicar com sucesso, marca o OutboxEvent como ENVIADO — sem retry aqui
  (isso é papel do Scheduler, MVP 3). README atualizado com o passo a passo
  de demo incluindo Kafka UI.
- Estado atual: **validado de ponta a ponta** — `docker-compose down -v` +
  `up -d --build` frio sobe tudo saudável; `POST /api/propostas` retorna
  `201`; `outbox_events` mostra o registro `status: ENVIADO` poucos ms depois
  da criação; a mensagem Avro chegou de fato no tópico `proposta-events`
  (confirmado via `kafka-console-consumer.sh` dentro do container, campos
  `propostaId/status/tipoAmortizacao/criadaEm` corretos). Kafka UI e Mongo
  Express respondendo 200. Dados de teste limpos, stack parado
  (`docker-compose down`, sem `-v`) ao final da sessão.
- **Gotchas técnicos importantes para as próximas sessões (evitar perder
  tempo redescobrindo)**:
  1. **Spring Boot 4.1 usa Jackson 3 por padrão** — o `ObjectMapper`
     autoconfigurado pelo Spring é `tools.jackson.databind.ObjectMapper`, NÃO
     `com.fasterxml.jackson.databind.ObjectMapper` (Jackson 2, que ainda está
     no classpath transitivamente só por compatibilidade de outras libs).
     Injetar o tipo errado quebra o boot da aplicação com
     `UnsatisfiedDependencyException` sem erro de compilação (Kotlin resolve
     o import de qualquer um dos dois pacotes). Sempre usar
     `tools.jackson.databind.ObjectMapper` ao injetar Jackson nesta stack.
  2. **CRLF quebra os scripts dentro dos containers Linux** — com
     `core.autocrlf=true` no Windows, um `git checkout`/`pull` reescreve
     `gradlew` e os scripts em `docker/**/*.sh` para CRLF, e o shebang
     `#!/bin/sh\r` falha com `not found` (mensagem enganosa — parece que o
     arquivo não existe, mas é problema de line ending). Corrigido
     definitivamente com [`.gitattributes`](./.gitattributes) forçando
     `eol=lf` nesses arquivos — qualquer novo script de shell adicionado ao
     projeto já cai na regra `*.sh`, não precisa lembrar de nada manualmente.
     Se algum build falhar de novo com "`./gradlew: not found`" ou script do
     compose morrendo sem log, suspeitar disso primeiro.
  3. `generateAvroJava` às vezes não roda junto de `compileKotlin` isolado no
     Gradle local (Windows) mesmo com o diretório gerado já existindo — rodar
     explicitamemte `./gradlew generateAvroJava compileKotlin` resolve. Dentro
     do Dockerfile isso não é um problema (o `bootJar` do build multi-stage já
     força a ordem certa via grafo de tasks completo).
- Próximo passo: **aguardar o usuário confirmar que o merge de
  `feature/mvp2-outbox-pattern` → `develop` foi feito**. Só então: `checkout
  develop` + `pull`, criar `feature/mvp3-scheduler-reprocessamento` (ou nome
  equivalente) a partir dela, e iniciar o MVP 3 (Scheduler de
  reprocessamento — claim atômico, backoff exponencial, FALHA_DEFINITIVA).

### 2026-08-08 — Processo de branch por MVP formalizado, aguardando merge do MVP 0+1
- Feito: usuário formalizou o fluxo de branches por MVP (seção "Perfil de
  trabalho" e fechamento do roadmap atualizados): a cada MVP, `pull` de
  `develop` → nova branch a partir dela para aquele MVP → commits granulares
  nessa branch → parar e só avançar para o MVP seguinte depois que o usuário
  confirmar que o merge daquela branch para `develop` foi feito (não basta
  aprovação verbal de "pode seguir" — é o merge em si que libera o próximo
  `pull`).
- Estado atual: branch `feature/mvp0-orquestracao-completa` (que acumulou
  MVP 0 + MVP 1, antes desta regra existir) ainda **não foi mergeada** em
  `develop` — PR aberto, aguardando o usuário mergear. Nenhum código novo foi
  escrito nesta entrada, só documentação de processo.
- Próximo passo: **aguardar o usuário confirmar que o merge de
  `feature/mvp0-orquestracao-completa` → `develop` foi feito**. Só então:
  `git checkout develop && git pull`, criar `feature/mvp2-...` a partir dela,
  e iniciar o MVP 2 (Outbox Pattern + fast-path pós-commit + Avro).

### 2026-08-08 — MVP 1 concluído, aguardando aprovação para MVP 2
- Feito: branch `feature/mvp0-orquestracao-completa` pushada para o remoto
  (PR aberto manualmente pelo usuário, link fornecido — `gh` CLI não está
  disponível neste ambiente). Na sequência, implementado o domínio `Proposta`
  completo nessa mesma branch: `Proposta` (Aggregate Root, `domain/model`),
  `StatusPropostaEnum` (EM_ANDAMENTO/PROCESSADA), `TipoAmortizacaoEnum`
  (SAC/PRICE), `CriarPropostaInputPort` + `CriarPropostaUsecase`,
  `PropostaOutputPort` + `PropostaRepositoryAdapter` (Spring Data MongoDB,
  `PropostaDocument` + `PropostaMongoRepository` + `PropostaMapper`),
  `PropostaController` (`POST /api/propostas`) + `PropostaRequestDto`/
  `PropostaResponseDto`, documentado no Swagger via anotações
  `@Operation`/`@Tag`/`@Schema`. README atualizado com seção "Como demonstrar
  — criação de Proposta" (Swagger → Mongo Express).
- Estado atual: **validado de ponta a ponta** — `docker-compose down -v` +
  `docker-compose up -d --build` frio sobe tudo saudável; `POST
  /api/propostas` com `{"tipoAmortizacao":"SAC"}` retorna `201` com a
  Proposta (`status: EM_ANDAMENTO`); documento confirmado na coleção
  `propostas` do Mongo via `mongosh` direto no container; Swagger UI
  responde 200 em `/swagger-ui/index.html`. Dados de teste limpos e stack
  parado (`docker-compose down`, sem `-v`) ao final da sessão — para religar,
  `docker-compose up -d`.
- Decisões/gotchas técnicos relevantes para a próxima sessão:
  - `PropostaController` depende só de `CriarPropostaInputPort` — **não** foi
    criado endpoint de consulta (`GET /api/propostas/{id}`) neste MVP, pois
    não havia porta de entrada definida para isso no roadmap e a demo do
    MVP 1 usa Mongo Express para mostrar o documento salvo, não a API. Se
    for necessário no futuro, criar um `BuscarPropostaInputPort` dedicado em
    vez de o controller chamar `PropostaOutputPort` direto (quebraria a
    regra de dependência hexagonal).
  - `PropostaOutputPort` ficou só com `salvar()` por ora (sem `buscarPorId`),
    pelo mesmo motivo acima — adicionar quando houver um consumidor real.
  - Ambiente não tem `gh` CLI instalado (nem Bash nem PowerShell) — abertura
    de PR precisa ser manual pelo link do GitHub ou instalar o CLI antes.
  - Build/testado com `./gradlew compileKotlin` (sem suíte de testes, por
    decisão do usuário) antes de subir o Docker — fluxo a repetir nos
    próximos MVPs para não gastar tempo de build de imagem em erro de
    compilação óbvio.
- Próximo passo: commitar o MVP 1 (commits granulares em português), push,
  **aguardar aprovação explícita do usuário** antes de abrir/atualizar PR e
  antes de iniciar o MVP 2 (Outbox Pattern + fast-path pós-commit + Avro).

### 2026-08-08 — MVP 0 concluído, aguardando aprovação para MVP 1
- Feito: esqueleto Kotlin/Gradle (pacotes hexagonais com stub), classe
  principal Spring Boot (`OutboxPatternDemonstrationApplication`),
  `application.yml`, `OpenApiConfig` (Swagger com título/descrição mesmo sem
  endpoints), `Dockerfile` multi-stage (build com Gradle wrapper + runtime JRE
  21 com healthcheck via curl), `docker-compose.yml` completo (Mongo
  replica-set single-node com init automático via container `mongo-init`,
  Kafka KRaft single-broker com criação automática dos tópicos
  `proposta-events`/`proposta-events.DLT` via container `kafka-init`,
  LocalStack com init hook nativo criando o secret
  `outbox-demo/credentials`, Kafka UI, Mongo Express), healthchecks em todos
  os serviços com `depends_on: condition: service_healthy` /
  `service_completed_successfully`. README.md reescrito com pré-requisitos,
  portas e como rodar/resetar a demo.
- Estado atual: **validado de ponta a ponta** — `docker-compose down -v` +
  `docker-compose up -d` frio sobe tudo saudável sozinho (mongo, kafka,
  localstack, kafka-ui, mongo-express healthy; app healthy ~15s depois dos
  demais). `/actuator/health` responde 200 com `mongo: UP`. Swagger, Kafka UI
  e Mongo Express abrem sem login (200) no navegador.
- Decisões/gotchas técnicos relevantes para a próxima sessão:
  - **Spring Boot 4.0+ depreciou `spring.data.mongodb.*` (nível error)** em
    favor de `spring.mongodb.*` — usar o namespace antigo não dá erro nem
    warning visível, só cai silenciosamente no default `localhost:27017` e
    quebra a conexão. `application.yml` já usa o namespace novo
    (`spring.mongodb.uri`); manter atenção nisso ao adicionar outras
    propriedades Mongo.
  - **mongo-express 1.x renomeou `ME_CONFIG_BASICAUTH_ENABLED` para
    `ME_CONFIG_BASICAUTH`** — a imagem vem com auth ligada por padrão
    (admin/pass); `docker-compose.yml` já usa o nome novo para abrir sem
    login.
  - Imagem Kafka usada: `apache/kafka:3.7.0` (KRaft nativo, sem Zookeeper).
    `CLUSTER_ID` fixo no compose (qualquer string base64 válida serve para
    single-node).
  - Trabalho feito na branch `feature/mvp0-orquestracao-completa` (a partir de
    `develop`) — ainda não commitado/pushado; branches `develop`/`staging`
    remotos existiam vazios (só README), criados em sessão anterior.
- Próximo passo: revisar/commitar o MVP 0 (commits granulares em português),
  **aguardar aprovação explícita do usuário** antes de abrir PR/push e antes
  de iniciar o MVP 1 (domínio `Proposta`).