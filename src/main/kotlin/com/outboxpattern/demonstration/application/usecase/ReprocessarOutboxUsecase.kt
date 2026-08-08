package com.outboxpattern.demonstration.application.usecase

import com.outboxpattern.demonstration.domain.event.PropostaCriadaEvent
import com.outboxpattern.demonstration.domain.model.OutboxEvent
import com.outboxpattern.demonstration.domain.port.input.ReprocessarOutboxInputPort
import com.outboxpattern.demonstration.domain.port.output.OutboxOutputPort
import com.outboxpattern.demonstration.domain.port.output.PropostaEventPublisherOutputPort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.time.Duration
import java.time.Instant
import kotlin.math.min
import kotlin.math.pow

/**
 * Fallback do Outbox Pattern: reivindica (claim atômico) e tenta republicar
 * OutboxEvents que o fast-path pós-commit (`CriarPropostaUsecase.aoConfirmarCriacao`)
 * não confirmou — crash entre commit e publish, Kafka indisponível no
 * momento da criação, etc. O caminho feliz nunca passa por aqui.
 */
@Service
class ReprocessarOutboxUsecase(
	private val outboxOutputPort: OutboxOutputPort,
	private val propostaEventPublisherOutputPort: PropostaEventPublisherOutputPort,
	private val objectMapper: ObjectMapper,
	@param:Value("\${outbox.scheduler.claim-ttl-segundos:30}") private val claimTtlSegundos: Long,
	@param:Value("\${outbox.scheduler.grace-period-segundos:6}") private val gracePeriodSegundos: Long,
	@param:Value("\${outbox.scheduler.max-tentativas:5}") private val maxTentativas: Int,
	@param:Value("\${outbox.scheduler.backoff-base-segundos:5}") private val backoffBaseSegundos: Long,
	@param:Value("\${outbox.scheduler.backoff-max-segundos:300}") private val backoffMaxSegundos: Long,
) : ReprocessarOutboxInputPort {

	override fun reprocessarPendentes(dono: String): Int {
		var processados = 0
		while (processados < LOTE_MAXIMO) {
			val outboxEvent = outboxOutputPort.reivindicarProximoPendente(
				dono,
				Duration.ofSeconds(claimTtlSegundos),
				Duration.ofSeconds(gracePeriodSegundos),
			) ?: break
			processarUm(outboxEvent)
			processados++
		}
		return processados
	}

	private fun processarUm(outboxEvent: OutboxEvent) {
		val id = requireNotNull(outboxEvent.id) { "OutboxEvent reivindicado sempre vem persistido, com id" }
		val evento = desserializar(outboxEvent)
		if (evento == null) {
			// eventType desconhecido ou payload corrompido: reprocessar de novo
			// não resolveria ("poison message") - vai direto ao estado terminal.
			log.warn("OutboxEvent {} com payload/eventType inválido ({}), indo para FALHA_DEFINITIVA", id, outboxEvent.eventType)
			outboxOutputPort.marcarComoFalhaDefinitiva(id, outboxEvent.tentativas)
			return
		}

		val publicado = runCatching { propostaEventPublisherOutputPort.publicar(evento) }.isSuccess
		if (publicado) {
			outboxOutputPort.marcarComoEnviadoPorId(id)
			return
		}

		val tentativas = outboxEvent.tentativas + 1
		if (tentativas >= maxTentativas) {
			log.warn("OutboxEvent {} esgotou {} tentativas, indo para FALHA_DEFINITIVA", id, maxTentativas)
			outboxOutputPort.marcarComoFalhaDefinitiva(id, tentativas)
		} else {
			val proximaTentativaEm = Instant.now().plus(calcularBackoff(tentativas))
			log.info("OutboxEvent {} falhou (tentativa {}/{}), reagendado para {}", id, tentativas, maxTentativas, proximaTentativaEm)
			outboxOutputPort.reagendarAposFalha(id, tentativas, proximaTentativaEm)
		}
	}

	private fun desserializar(outboxEvent: OutboxEvent): PropostaCriadaEvent? {
		if (outboxEvent.eventType != PropostaCriadaEvent.TIPO_EVENTO) return null
		return runCatching { objectMapper.readValue(outboxEvent.payload, PropostaCriadaEvent::class.java) }.getOrNull()
	}

	/**
	 * Backoff exponencial com teto: dobra a cada tentativa (5s, 10s, 20s...)
	 * até `backoffMaxSegundos` — evita tanto martelar um Kafka fora do ar
	 * quanto esperar tempo demais depois que ele volta.
	 */
	private fun calcularBackoff(tentativas: Int): Duration {
		val segundos = (backoffBaseSegundos * 2.0.pow(tentativas - 1)).toLong()
		return Duration.ofSeconds(min(segundos, backoffMaxSegundos))
	}

	companion object {
		// Teto de segurança por rodada do Scheduler - evita que um pico de
		// eventos PENDENTE prenda uma única execução por tempo indefinido.
		private const val LOTE_MAXIMO = 50
		private val log = LoggerFactory.getLogger(ReprocessarOutboxUsecase::class.java)
	}
}
