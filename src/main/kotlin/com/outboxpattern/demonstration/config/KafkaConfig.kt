package com.outboxpattern.demonstration.config

import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory

/**
 * Producer de bytes crus: a codificação Avro do payload acontece no próprio
 * `PropostaProducer` (sem Schema Registry, ver CLAUDE.md — "Stack
 * confirmada"), então o `value.serializer` aqui só repassa o array de bytes
 * já pronto — não há (de)serialização Avro automática via configuração.
 */
@Configuration
class KafkaConfig(
	@param:Value("\${spring.kafka.bootstrap-servers}")
	private val bootstrapServers: String,
) {

	@Bean
	fun propostaProducerFactory(): ProducerFactory<String, ByteArray> {
		val props = mapOf(
			ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
			ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
			ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to ByteArraySerializer::class.java,
			// Default do client é 60s - inaceitável tanto pro fast-path (travaria
			// a request HTTP) quanto pro Scheduler (travaria a rodada inteira)
			// quando o Kafka está fora do ar. Falha rápido e deixa o OutboxEvent
			// PENDENTE para o Scheduler de fallback (MVP 3) tentar de novo.
			ProducerConfig.MAX_BLOCK_MS_CONFIG to "3000",
		)
		return DefaultKafkaProducerFactory(props)
	}

	@Bean
	fun propostaKafkaTemplate(propostaProducerFactory: ProducerFactory<String, ByteArray>): KafkaTemplate<String, ByteArray> =
		KafkaTemplate(propostaProducerFactory)
}
