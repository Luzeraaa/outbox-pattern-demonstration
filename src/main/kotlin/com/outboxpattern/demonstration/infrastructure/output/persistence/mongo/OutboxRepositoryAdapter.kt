package com.outboxpattern.demonstration.infrastructure.output.persistence.mongo

import com.outboxpattern.demonstration.domain.model.OutboxEvent
import com.outboxpattern.demonstration.domain.model.OutboxStatusEnum
import com.outboxpattern.demonstration.domain.port.output.OutboxOutputPort
import com.outboxpattern.demonstration.infrastructure.output.persistence.mongo.mapper.OutboxMapper
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

/**
 * Implementa `OutboxOutputPort` usando Spring Data MongoDB. Updates pontuais
 * (`marcarComoEnviado*`, `reagendarAposFalha`, `marcarComoFalhaDefinitiva`)
 * usam `MongoTemplate` direto, não o `OutboxMongoRepository` — não faz
 * sentido carregar o documento inteiro em memória só para trocar status.
 */
@Component
class OutboxRepositoryAdapter(
	private val repository: OutboxMongoRepository,
	private val mongoTemplate: MongoTemplate,
) : OutboxOutputPort {

	override fun salvar(outboxEvent: OutboxEvent): OutboxEvent {
		val document = OutboxMapper.paraDocument(outboxEvent)
		val salvo = repository.save(document)
		return OutboxMapper.paraDominio(salvo)
	}

	override fun marcarComoEnviado(aggregateId: String) {
		val query = Query(
			Criteria.where("aggregateId").`is`(aggregateId)
				.and("status").`is`(OutboxStatusEnum.PENDENTE.name),
		)
		mongoTemplate.updateFirst(query, statusEnviadoUpdate(), OutboxDocument::class.java)
	}

	override fun reivindicarProximoPendente(dono: String, ttlClaim: Duration, graceDesdeCriacao: Duration): OutboxEvent? {
		val agora = Instant.now()

		// PENDENTE que já teve tentativa (tem backoff) e o backoff já venceu...
		val reentativaElegivel = Criteria().andOperator(
			Criteria.where("status").`is`(OutboxStatusEnum.PENDENTE.name),
			Criteria.where("proximaTentativaEm").lte(agora),
		)
		// ...OU PENDENTE de 1ª tentativa, mas só depois de "envelhecer" pelo
		// período de carência - dá tempo do fast-path pós-commit terminar
		// antes do Scheduler competir pelo mesmo registro (sem isso, quase
		// toda criação vira publicação duplicada, não só falhas genuínas).
		val primeiraTentativaElegivel = Criteria().andOperator(
			Criteria.where("status").`is`(OutboxStatusEnum.PENDENTE.name),
			Criteria.where("proximaTentativaEm").exists(false),
			Criteria.where("createdAt").lte(agora.minus(graceDesdeCriacao)),
		)
		// ...OU EM_PROCESSAMENTO cujo claim expirou (a instância anterior
		// provavelmente crashou no meio do reprocessamento).
		val claimExpirado = Criteria().andOperator(
			Criteria.where("status").`is`(OutboxStatusEnum.EM_PROCESSAMENTO.name),
			Criteria.where("claimExpiraEm").lt(agora),
		)

		val query = Query(Criteria().orOperator(reentativaElegivel, primeiraTentativaElegivel, claimExpirado))
			.with(Sort.by(Sort.Direction.ASC, "createdAt"))

		val update = Update()
			.set("status", OutboxStatusEnum.EM_PROCESSAMENTO.name)
			.set("claimedBy", dono)
			.set("claimExpiraEm", agora.plus(ttlClaim))
			.set("updatedAt", agora)

		// findAndModify é atômico no Mongo: mesmo com duas instâncias da app
		// rodando a mesma query ao mesmo tempo, só uma consegue "vencer" o
		// claim de um dado documento - é isso que evita publicação duplicada.
		val options = FindAndModifyOptions.options().returnNew(true)
		val document = mongoTemplate.findAndModify(query, update, options, OutboxDocument::class.java)
		return document?.let(OutboxMapper::paraDominio)
	}

	override fun marcarComoEnviadoPorId(id: String) {
		atualizarPorId(id, statusEnviadoUpdate())
	}

	override fun reagendarAposFalha(id: String, tentativas: Int, proximaTentativaEm: Instant) {
		atualizarPorId(
			id,
			Update()
				.set("status", OutboxStatusEnum.PENDENTE.name)
				.set("tentativas", tentativas)
				.set("proximaTentativaEm", proximaTentativaEm)
				.set("claimedBy", null)
				.set("claimExpiraEm", null),
		)
	}

	override fun marcarComoFalhaDefinitiva(id: String, tentativas: Int) {
		// Estado terminal: limpa o claim também - o registro não pertence
		// mais a nenhuma instância, só fica ali para investigação manual.
		atualizarPorId(
			id,
			Update()
				.set("status", OutboxStatusEnum.FALHA_DEFINITIVA.name)
				.set("tentativas", tentativas)
				.set("claimedBy", null)
				.set("claimExpiraEm", null),
		)
	}

	/** ENVIADO é sempre estado terminal de sucesso - limpa o claim junto,
	 * tanto no caminho do fast-path (`marcarComoEnviado`) quanto no do
	 * Scheduler (`marcarComoEnviadoPorId`), para o documento não sobrar com
	 * bookkeeping de reprocessamento que já deixou de fazer sentido. */
	private fun statusEnviadoUpdate(): Update =
		Update()
			.set("status", OutboxStatusEnum.ENVIADO.name)
			.set("claimedBy", null)
			.set("claimExpiraEm", null)

	private fun atualizarPorId(id: String, update: Update) {
		val query = Query(Criteria.where("id").`is`(id))
		mongoTemplate.updateFirst(query, update.set("updatedAt", Instant.now()), OutboxDocument::class.java)
	}
}
