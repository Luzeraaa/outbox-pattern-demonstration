package com.outboxpattern.demonstration.application.usecase

import com.outboxpattern.demonstration.domain.event.PropostaCriadaEvent
import com.outboxpattern.demonstration.domain.model.OutboxEvent
import com.outboxpattern.demonstration.domain.model.Proposta
import com.outboxpattern.demonstration.domain.model.TipoAmortizacaoEnum
import com.outboxpattern.demonstration.domain.port.input.CriarPropostaInputPort
import com.outboxpattern.demonstration.domain.port.output.OutboxOutputPort
import com.outboxpattern.demonstration.domain.port.output.PropostaEventPublisherOutputPort
import com.outboxpattern.demonstration.domain.port.output.PropostaOutputPort
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import tools.jackson.databind.ObjectMapper

/**
 * Orquestra a criação de uma Proposta e o núcleo do Outbox Pattern: Proposta
 * e OutboxEvent são gravados na MESMA transação Mongo (`@Transactional` —
 * "é aqui que mora a transação", ver CLAUDE.md). A publicação em si só
 * acontece DEPOIS do commit: [aoConfirmarCriacao] reage ao evento de
 * aplicação em memória publicado ao final de [criar], evitando publicar no
 * Kafka algo que a transação ainda poderia reverter.
 */
@Service
class CriarPropostaUsecase(
	private val propostaOutputPort: PropostaOutputPort,
	private val outboxOutputPort: OutboxOutputPort,
	private val propostaEventPublisherOutputPort: PropostaEventPublisherOutputPort,
	private val applicationEventPublisher: ApplicationEventPublisher,
	private val objectMapper: ObjectMapper,
) : CriarPropostaInputPort {

	@Transactional
	override fun criar(tipoAmortizacao: TipoAmortizacaoEnum): Proposta {
		val proposta = propostaOutputPort.salvar(Proposta.nova(tipoAmortizacao))
		val evento = PropostaCriadaEvent.de(proposta)

		outboxOutputPort.salvar(
			OutboxEvent.pendente(
				aggregateId = evento.propostaId,
				eventType = PropostaCriadaEvent.TIPO_EVENTO,
				payload = objectMapper.writeValueAsString(evento),
			),
		)

		// Só publicado em memória aqui; quem efetivamente dispara o Kafka é o
		// listener AFTER_COMMIT abaixo, e só roda se a transação commitar.
		applicationEventPublisher.publishEvent(evento)
		return proposta
	}

	/**
	 * Fast-path do Outbox Pattern: publica no Kafka assim que a transação que
	 * criou a Proposta é confirmada. Se a publicação falhar aqui, o
	 * OutboxEvent permanece PENDENTE — não há retry nesta chamada, essa
	 * responsabilidade é do Scheduler de fallback (MVP 3), evitando lógica de
	 * reprocessamento duplicada em dois lugares.
	 */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	fun aoConfirmarCriacao(evento: PropostaCriadaEvent) {
		runCatching { propostaEventPublisherOutputPort.publicar(evento) }
			.onSuccess { outboxOutputPort.marcarComoEnviado(evento.propostaId) }
	}
}
