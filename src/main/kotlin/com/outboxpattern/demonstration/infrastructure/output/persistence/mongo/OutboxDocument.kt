package com.outboxpattern.demonstration.infrastructure.output.persistence.mongo

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

/**
 * Modelo espelho de persistência do OutboxEvent, coleção "outbox_events".
 * Salvo na mesma transação Mongo do `PropostaDocument` que o originou (ver
 * `CriarPropostaUsecase`) — a conversão para/do domínio é feita só pelo
 * `OutboxMapper`.
 */
@Document(collection = "outbox_events")
data class OutboxDocument(
	@Id
	val id: String? = null,
	val aggregateId: String,
	val eventType: String,
	val payload: String,
	val status: String,
	val tentativas: Int,
	val createdAt: Instant,
	val updatedAt: Instant,
)
