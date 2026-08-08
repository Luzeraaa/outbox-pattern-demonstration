package com.outboxpattern.demonstration.domain.port.output

import com.outboxpattern.demonstration.domain.model.OutboxEvent
import java.time.Duration
import java.time.Instant

/**
 * Porta de saída para persistência do registro de outbox — contrato que a
 * infraestrutura precisa cumprir (hoje: `OutboxRepositoryAdapter`) para que
 * Proposta e OutboxEvent sejam gravados na mesma transação Mongo, núcleo do
 * Outbox Pattern, e para que o fallback (Scheduler, MVP 3) consiga
 * reivindicar e reprocessar registros pendentes com segurança.
 */
interface OutboxOutputPort {
	fun salvar(outboxEvent: OutboxEvent): OutboxEvent

	/**
	 * Marca como ENVIADO o OutboxEvent PENDENTE mais recente do agregado
	 * informado. Usado pelo fast-path pós-commit (`CriarPropostaUsecase`)
	 * assim que a publicação no Kafka é confirmada com sucesso.
	 */
	fun marcarComoEnviado(aggregateId: String)

	/**
	 * Reivindica atomicamente (via `findAndModify`) o próximo OutboxEvent
	 * elegível: PENDENTE com backoff já vencido, PENDENTE "de primeira
	 * tentativa" mas mais velho que [graceDesdeCriacao] (dá tempo do
	 * fast-path pós-commit terminar antes do Scheduler competir pelo mesmo
	 * registro — sem isso, o Scheduler duplica a publicação em quase toda
	 * criação, não só nas falhas genuínas), ou EM_PROCESSAMENTO cujo claim
	 * expirou (cobre o caso de a instância anterior ter crashado no meio do
	 * reprocessamento). Retorna `null` quando não há mais nada elegível
	 * nesta rodada.
	 */
	fun reivindicarProximoPendente(dono: String, ttlClaim: Duration, graceDesdeCriacao: Duration): OutboxEvent?

	/** Publicação confirmada pelo Scheduler: encerra o ciclo de vida com sucesso. */
	fun marcarComoEnviadoPorId(id: String)

	/**
	 * Registra uma tentativa falha do Scheduler: volta para PENDENTE com o
	 * novo número de tentativas e a próxima janela de backoff, liberando o claim.
	 */
	fun reagendarAposFalha(id: String, tentativas: Int, proximaTentativaEm: Instant)

	/** Estado terminal: limite de tentativas esgotado (ou payload/eventType
	 * inválido) — não é mais reprocessado automaticamente. Recebe
	 * [tentativas] para que o contador final fique visível no registro (sem
	 * isso, a última tentativa que estourou o limite nunca seria persistida). */
	fun marcarComoFalhaDefinitiva(id: String, tentativas: Int)
}
