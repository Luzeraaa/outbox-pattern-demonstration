package com.outboxpattern.demonstration.infrastructure.scheduler

import com.outboxpattern.demonstration.domain.port.input.ReprocessarOutboxInputPort
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Fallback do Outbox Pattern: cobre o que o fast-path pós-commit
 * (`CriarPropostaUsecase.aoConfirmarCriacao`) não conseguiu confirmar — crash
 * entre commit e publish, Kafka indisponível no momento da criação, etc. É o
 * ÚNICO ponto do sistema que faz *polling* na coleção outbox; o fast-path
 * nunca reconsulta o banco (ver CLAUDE.md, "Stack confirmada").
 *
 * Opt-in via `outbox.scheduler.enabled` (default `true`) — desabilitar não
 * quebra o fluxo síncrono de criação, só remove a rede de segurança contra
 * falhas transitórias do Kafka.
 */
@Component
@ConditionalOnProperty(prefix = "outbox.scheduler", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class OutboxReprocessamentoScheduler(
	private val reprocessarOutboxInputPort: ReprocessarOutboxInputPort,
) {

	// Identifica esta instância como "dona" do claim atômico (ver
	// OutboxOutputPort.reivindicarProximoPendente) - necessário para que
	// múltiplas instâncias da app não publiquem o mesmo evento duas vezes.
	private val dono = "scheduler-${UUID.randomUUID().toString().take(8)}"

	@Scheduled(fixedDelayString = "\${outbox.scheduler.fixed-delay-ms:5000}")
	fun reprocessar() {
		val processados = reprocessarOutboxInputPort.reprocessarPendentes(dono)
		if (processados > 0) {
			log.info("{} OutboxEvent(s) reprocessado(s) por {}", processados, dono)
		}
	}

	companion object {
		private val log = LoggerFactory.getLogger(OutboxReprocessamentoScheduler::class.java)
	}
}
