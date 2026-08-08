package com.outboxpattern.demonstration.application.usecase

import com.outboxpattern.demonstration.domain.model.Proposta
import com.outboxpattern.demonstration.domain.model.TipoAmortizacaoEnum
import com.outboxpattern.demonstration.domain.port.input.CriarPropostaInputPort
import com.outboxpattern.demonstration.domain.port.output.PropostaOutputPort
import org.springframework.stereotype.Service

/**
 * Orquestra a criação de uma Proposta. Neste MVP a persistência é apenas da
 * Proposta em si; a gravação do `OutboxEvent` na mesma transação Mongo (o
 * núcleo do Outbox Pattern) chega no MVP 2 — este usecase é o ponto onde essa
 * transação será estendida, por isso já existe isolado desde já.
 */
@Service
class CriarPropostaUsecase(
	private val propostaOutputPort: PropostaOutputPort,
) : CriarPropostaInputPort {

	override fun criar(tipoAmortizacao: TipoAmortizacaoEnum): Proposta {
		val proposta = Proposta.nova(tipoAmortizacao)
		return propostaOutputPort.salvar(proposta)
	}
}
