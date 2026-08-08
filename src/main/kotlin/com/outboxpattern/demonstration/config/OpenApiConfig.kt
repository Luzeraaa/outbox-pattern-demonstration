package com.outboxpattern.demonstration.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Metadados exibidos no Swagger UI. Existe desde o MVP 0 (mesmo sem
 * endpoints ainda) porque a liderança abre o Swagger como primeiro passo da
 * demo — a tela não pode aparecer com título genérico "OpenAPI definition".
 */
@Configuration
class OpenApiConfig {

	@Bean
	fun outboxPatternOpenApi(): OpenAPI =
		OpenAPI().info(
			Info()
				.title("Outbox Pattern — Demonstração")
				.description(
					"POC que demonstra o Outbox Pattern: criação de Proposta, publicação " +
						"confiável de eventos no Kafka via Mongo (fast-path + scheduler de " +
						"fallback) e consumo idempotente com Circuit Breaker/Retry/DLT."
				)
				.version("0.0.1-SNAPSHOT")
		)
}
