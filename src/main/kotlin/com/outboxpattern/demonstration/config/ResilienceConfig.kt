package com.outboxpattern.demonstration.config

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.retry.RetryRegistry
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration

/**
 * Observabilidade do Circuit Breaker/Retry "processar-proposta" (limiares
 * configurados via `application.yml`, `resilience4j.circuitbreaker.instances`
 * / `resilience4j.retry.instances` — o starter já os aplica automaticamente
 * por nome, nenhuma config extra é necessária para o Circuit Breaker/Retry
 * funcionarem). `ProcessarPropostaUsecase` só loga sucesso/idempotência; sem
 * isso, uma falha (ex.: Mongo indisponível) seria silenciosa até o resultado
 * final na DLT — aqui logamos cada tentativa e toda transição de estado do
 * circuito, para a demo mostrar claramente o retry acontecendo e quando o
 * circuito abre (dependência instável) e fecha de novo.
 */
@Configuration
class ResilienceConfig(
	circuitBreakerRegistry: CircuitBreakerRegistry,
	retryRegistry: RetryRegistry,
) {

	init {
		circuitBreakerRegistry.circuitBreaker(INSTANCIA)
			.eventPublisher
			.onStateTransition { evento ->
				log.info(
					"[CircuitBreaker {}] {} -> {}",
					INSTANCIA,
					evento.stateTransition.fromState,
					evento.stateTransition.toState,
				)
			}

		retryRegistry.retry(INSTANCIA)
			.eventPublisher
			.onRetry { evento ->
				log.warn(
					"[Retry {}] tentativa {} falhou: {}",
					INSTANCIA,
					evento.numberOfRetryAttempts,
					evento.lastThrowable?.message,
				)
			}
	}

	companion object {
		private const val INSTANCIA = "processar-proposta"
		private val log = LoggerFactory.getLogger(ResilienceConfig::class.java)
	}
}
