package com.outboxpattern.demonstration.application.usecase

import com.outboxpattern.demonstration.domain.exception.PropostaNaoEncontradaException
import com.outboxpattern.demonstration.domain.model.StatusPropostaEnum
import com.outboxpattern.demonstration.domain.port.input.ProcessarPropostaInputPort
import com.outboxpattern.demonstration.domain.port.output.PropostaOutputPort
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Processa o evento `PropostaCriada` consumido do Kafka (`PropostaListener`):
 * transiciona a Proposta de EM_ANDAMENTO para PROCESSADA. Idempotente por
 * construção — o Outbox Pattern só garante *at-least-once delivery*, então
 * receber a mesma mensagem mais de uma vez é esperado, não um erro; por isso
 * checa o status atual antes de agir, em vez de aplicar a transição às cegas.
 *
 * `@Retry` (inner) tenta de novo em falhas transitórias antes de desistir;
 * `@CircuitBreaker` (outer) avalia o resultado da sequência inteira de
 * tentativas e abre o circuito se as falhas persistirem, evitando martelar
 * uma dependência já sabidamente indisponível. Se mesmo assim a exceção
 * propagar, quem decide o destino final (DLT) é o error handler do container
 * Kafka (ver `KafkaConfig`), não este usecase.
 */
@Service
class ProcessarPropostaUsecase(
	private val propostaOutputPort: PropostaOutputPort,
) : ProcessarPropostaInputPort {

	@CircuitBreaker(name = "processar-proposta")
	@Retry(name = "processar-proposta")
	override fun processar(propostaId: String) {
		val proposta = propostaOutputPort.buscarPorId(propostaId)
			?: throw PropostaNaoEncontradaException(propostaId)

		if (proposta.status == StatusPropostaEnum.PROCESSADA) {
			log.info("Proposta {} já está PROCESSADA — mensagem duplicada ignorada (at-least-once esperado)", propostaId)
			return
		}

		propostaOutputPort.salvar(proposta.copy(status = StatusPropostaEnum.PROCESSADA))
		log.info("Proposta {} processada com sucesso", propostaId)
	}

	companion object {
		private val log = LoggerFactory.getLogger(ProcessarPropostaUsecase::class.java)
	}
}
