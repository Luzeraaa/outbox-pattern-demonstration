package com.outboxpattern.demonstration.infrastructure.output.persistence.mongo.mapper

import com.outboxpattern.demonstration.domain.model.OutboxEvent
import com.outboxpattern.demonstration.domain.model.OutboxStatusEnum
import com.outboxpattern.demonstration.infrastructure.output.persistence.mongo.OutboxDocument

/**
 * Converte OutboxEvent (domínio) ↔ OutboxDocument (persistência Mongo).
 * Único lugar do sistema que sabe fazer essa tradução — mantém o domínio
 * livre de qualquer dependência de infraestrutura.
 */
object OutboxMapper {

	fun paraDocument(outboxEvent: OutboxEvent): OutboxDocument =
		OutboxDocument(
			id = outboxEvent.id,
			aggregateId = outboxEvent.aggregateId,
			eventType = outboxEvent.eventType,
			payload = outboxEvent.payload,
			status = outboxEvent.status.name,
			tentativas = outboxEvent.tentativas,
			createdAt = outboxEvent.createdAt,
			updatedAt = outboxEvent.updatedAt,
		)

	fun paraDominio(document: OutboxDocument): OutboxEvent =
		OutboxEvent(
			id = document.id,
			aggregateId = document.aggregateId,
			eventType = document.eventType,
			payload = document.payload,
			status = OutboxStatusEnum.valueOf(document.status),
			tentativas = document.tentativas,
			createdAt = document.createdAt,
			updatedAt = document.updatedAt,
		)
}
