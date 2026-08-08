package com.outboxpattern.demonstration.domain.model

import java.time.Instant

/**
 * Aggregate Root do domínio de crédito desta POC. Modela uma proposta de
 * financiamento cujo processamento é assíncrono via Outbox Pattern: a criação
 * apenas registra a intenção (status [StatusPropostaEnum.EM_ANDAMENTO]); só é
 * considerada concluída quando o evento correspondente é consumido com
 * sucesso do lado do listener (MVP 4). Classe pura — sem anotação de
 * Mongo/Spring, ver `PropostaDocument`/`PropostaMapper` para a persistência.
 */
data class Proposta(
	val id: String? = null,
	val status: StatusPropostaEnum,
	val tipoAmortizacao: TipoAmortizacaoEnum,
	val criadaEm: Instant = Instant.now(),
) {
	companion object {
		/**
		 * Única forma de criação de uma Proposta nova: força o status inicial
		 * [StatusPropostaEnum.EM_ANDAMENTO], evitando que qualquer chamador
		 * construa uma Proposta já como PROCESSADA sem ter passado pelo fluxo
		 * de eventos.
		 */
		fun nova(tipoAmortizacao: TipoAmortizacaoEnum): Proposta =
			Proposta(status = StatusPropostaEnum.EM_ANDAMENTO, tipoAmortizacao = tipoAmortizacao)
	}
}
