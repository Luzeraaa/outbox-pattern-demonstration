package com.outboxpattern.demonstration.domain.port.output

import com.outboxpattern.demonstration.domain.model.OutboxEvent

/**
 * Porta de saída para persistência do registro de outbox — contrato que a
 * infraestrutura precisa cumprir (hoje: `OutboxRepositoryAdapter`) para que
 * Proposta e OutboxEvent sejam gravados na mesma transação Mongo, núcleo do
 * Outbox Pattern.
 */
interface OutboxOutputPort {
	fun salvar(outboxEvent: OutboxEvent): OutboxEvent

	/**
	 * Marca como ENVIADO o OutboxEvent PENDENTE mais recente do agregado
	 * informado. Usado pelo fast-path pós-commit assim que a publicação no
	 * Kafka é confirmada com sucesso — dispensa reconsultar o outbox inteiro.
	 */
	fun marcarComoEnviado(aggregateId: String)
}
