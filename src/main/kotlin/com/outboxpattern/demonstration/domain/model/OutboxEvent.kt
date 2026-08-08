package com.outboxpattern.demonstration.domain.model

import java.time.Instant

/**
 * Registro de outbox: garante que a publicação de um evento de domínio (nesta
 * POC, sempre um `PropostaCriadaEvent`) só é tentada depois que a transação
 * que originou o evento foi de fato commitada — por isso é persistido na
 * MESMA transação Mongo do agregado que o originou (ver `CriarPropostaUsecase`).
 *
 * [payload] guarda a representação serializada do evento de domínio para que
 * o fallback (Scheduler, MVP 3) consiga republicar sem reconsultar o
 * agregado original — o outbox é autossuficiente para reenvio.
 */
data class OutboxEvent(
	val id: String? = null,
	val aggregateId: String,
	val eventType: String,
	val payload: String,
	val status: OutboxStatusEnum = OutboxStatusEnum.PENDENTE,
	val tentativas: Int = 0,
	val createdAt: Instant = Instant.now(),
	val updatedAt: Instant = Instant.now(),
) {
	companion object {
		/** Todo OutboxEvent nasce PENDENTE — só o fast-path/Scheduler o transicionam. */
		fun pendente(aggregateId: String, eventType: String, payload: String): OutboxEvent =
			OutboxEvent(aggregateId = aggregateId, eventType = eventType, payload = payload)
	}
}
