package com.outboxpattern.demonstration.infrastructure.output.persistence.mongo.mapper

import com.outboxpattern.demonstration.domain.model.Proposta
import com.outboxpattern.demonstration.domain.model.StatusPropostaEnum
import com.outboxpattern.demonstration.domain.model.TipoAmortizacaoEnum
import com.outboxpattern.demonstration.infrastructure.output.persistence.mongo.PropostaDocument

/**
 * Converte Proposta (domínio) ↔ PropostaDocument (persistência Mongo). Único
 * lugar do sistema que sabe fazer essa tradução — mantém o domínio livre de
 * qualquer dependência de infraestrutura.
 */
object PropostaMapper {

	fun paraDocument(proposta: Proposta): PropostaDocument =
		PropostaDocument(
			id = proposta.id,
			status = proposta.status.name,
			tipoAmortizacao = proposta.tipoAmortizacao.name,
			criadaEm = proposta.criadaEm,
		)

	fun paraDominio(document: PropostaDocument): Proposta =
		Proposta(
			id = document.id,
			status = StatusPropostaEnum.valueOf(document.status),
			tipoAmortizacao = TipoAmortizacaoEnum.valueOf(document.tipoAmortizacao),
			criadaEm = document.criadaEm,
		)
}
