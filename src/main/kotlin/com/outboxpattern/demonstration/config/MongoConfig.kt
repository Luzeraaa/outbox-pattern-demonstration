package com.outboxpattern.demonstration.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.MongoDatabaseFactory
import org.springframework.data.mongodb.MongoTransactionManager

/**
 * Habilita transação multi-documento no Mongo. Sem este bean, o
 * `@Transactional` do `CriarPropostaUsecase` não teria efeito e Proposta +
 * OutboxEvent seriam gravados em operações independentes — quebrando a
 * garantia central do Outbox Pattern (evento só existe se o agregado também
 * existir, e vice-versa). Exige replica-set, já configurado no `application.yml`.
 */
@Configuration
class MongoConfig {

	@Bean
	fun transactionManager(dbFactory: MongoDatabaseFactory): MongoTransactionManager =
		MongoTransactionManager(dbFactory)
}
