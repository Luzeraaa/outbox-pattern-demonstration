package com.outboxpattern.demonstration

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * Ponto de entrada da POC de demonstração do Outbox Pattern.
 *
 * `@EnableScheduling` já é habilitado aqui desde o MVP 0 porque o
 * `OutboxReprocessamentoScheduler` (MVP 3) depende dele — evita esquecer a
 * anotação quando o scheduler for de fato implementado.
 */
@SpringBootApplication
@EnableScheduling
class OutboxPatternDemonstrationApplication

fun main(args: Array<String>) {
	runApplication<OutboxPatternDemonstrationApplication>(*args)
}
