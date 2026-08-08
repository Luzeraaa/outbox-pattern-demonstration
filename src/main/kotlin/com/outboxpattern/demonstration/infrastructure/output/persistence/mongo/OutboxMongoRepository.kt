package com.outboxpattern.demonstration.infrastructure.output.persistence.mongo

import org.springframework.data.mongodb.repository.MongoRepository

/**
 * Interface Spring Data MongoDB para a coleção "outbox_events". Uso interno
 * do `OutboxRepositoryAdapter` — a atualização pontual de status usa
 * `MongoTemplate` diretamente no adapter, não este repositório (ver
 * `marcarComoEnviado`).
 */
interface OutboxMongoRepository : MongoRepository<OutboxDocument, String>
