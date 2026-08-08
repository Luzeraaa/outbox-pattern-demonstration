package com.outboxpattern.demonstration.infrastructure.output.persistence.mongo

import com.outboxpattern.demonstration.domain.model.OutboxEvent
import com.outboxpattern.demonstration.domain.model.OutboxStatusEnum
import com.outboxpattern.demonstration.domain.port.output.OutboxOutputPort
import com.outboxpattern.demonstration.infrastructure.output.persistence.mongo.mapper.OutboxMapper
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Implementa `OutboxOutputPort` usando Spring Data MongoDB. `marcarComoEnviado`
 * usa `MongoTemplate` (não o `OutboxMongoRepository`) porque é um update
 * pontual e direcionado — não faz sentido carregar o documento inteiro em
 * memória só para trocar um campo de status.
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
		val update = Update()
			.set("status", OutboxStatusEnum.ENVIADO.name)
			.set("updatedAt", Instant.now())
		mongoTemplate.updateFirst(query, update, OutboxDocument::class.java)
	}
}
