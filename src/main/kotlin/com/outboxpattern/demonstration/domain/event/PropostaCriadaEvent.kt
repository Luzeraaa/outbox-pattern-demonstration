package com.outboxpattern.demonstration.domain.event

import com.outboxpattern.demonstration.domain.model.Proposta
import com.outboxpattern.demonstration.domain.model.StatusPropostaEnum
import com.outboxpattern.demonstration.domain.model.TipoAmortizacaoEnum
import java.time.Instant

/**
 * Domain Event: fato de negócio imutável de que uma Proposta foi criada. É
 * exatamente o payload publicado no tópico Kafka `proposta-events` (via
 * Avro, ver `PropostaProducer`) — carrega tudo que o consumidor precisa (id,
 * status, tipoAmortizacao) para que o listener (MVP 4) nunca precise
 * reconsultar a Proposta no Mongo para montar sua reação.
 */
data class PropostaCriadaEvent(
	val propostaId: String,
	val status: StatusPropostaEnum,
	val tipoAmortizacao: TipoAmortizacaoEnum,
	val criadaEm: Instant,
) {
	companion object {
		/** Nome do evento gravado em `OutboxEvent.eventType` — identifica o
		 * payload para quem for reprocessar o outbox (Scheduler, MVP 3). */
		const val TIPO_EVENTO = "PropostaCriada"

		fun de(proposta: Proposta): PropostaCriadaEvent {
			val id = requireNotNull(proposta.id) { "Só é possível gerar o evento após a Proposta ser persistida" }
			return PropostaCriadaEvent(
				propostaId = id,
				status = proposta.status,
				tipoAmortizacao = proposta.tipoAmortizacao,
				criadaEm = proposta.criadaEm,
			)
		}
	}
}
