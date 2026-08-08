package com.outboxpattern.demonstration.infrastructure.output.persistence.mongo

import com.outboxpattern.demonstration.domain.model.Proposta
import com.outboxpattern.demonstration.domain.port.output.PropostaOutputPort
import com.outboxpattern.demonstration.infrastructure.output.persistence.mongo.mapper.PropostaMapper
import org.springframework.stereotype.Component

/**
 * Implementa `PropostaOutputPort` usando Spring Data MongoDB. Único ponto da
 * aplicação que conhece `PropostaMongoRepository` — a camada de aplicação
 * enxerga apenas a porta, nunca este adapter diretamente.
 */
@Component
class PropostaRepositoryAdapter(
	private val repository: PropostaMongoRepository,
) : PropostaOutputPort {

	override fun salvar(proposta: Proposta): Proposta {
		val document = PropostaMapper.paraDocument(proposta)
		val salvo = repository.save(document)
		return PropostaMapper.paraDominio(salvo)
	}

	override fun buscarPorId(id: String): Proposta? =
		repository.findById(id).map(PropostaMapper::paraDominio).orElse(null)
}
