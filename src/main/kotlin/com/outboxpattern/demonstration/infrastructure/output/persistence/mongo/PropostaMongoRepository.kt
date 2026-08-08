package com.outboxpattern.demonstration.infrastructure.output.persistence.mongo

import org.springframework.data.mongodb.repository.MongoRepository

/**
 * Interface Spring Data MongoDB para a coleção "propostas". Uso interno do
 * `PropostaRepositoryAdapter` — nunca referenciada fora da infraestrutura,
 * para que a aplicação dependa só de `PropostaOutputPort`.
 */
interface PropostaMongoRepository : MongoRepository<PropostaDocument, String>
