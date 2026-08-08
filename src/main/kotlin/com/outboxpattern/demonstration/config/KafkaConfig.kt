package com.outboxpattern.demonstration.config

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.apache.kafka.common.TopicPartition
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.util.backoff.FixedBackOff

/**
 * Producer de bytes crus: a codificação Avro do payload acontece no próprio
 * `PropostaProducer` (sem Schema Registry, ver CLAUDE.md — "Stack
 * confirmada"), então o `value.serializer` aqui só repassa o array de bytes
 * já pronto — não há (de)serialização Avro automática via configuração. O
 * consumer segue o mesmo raciocínio: `PropostaListener` desserializa Avro
 * manualmente, então o `value.deserializer` também só repassa bytes.
 */
@Configuration
@EnableKafka
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

	@Bean
	fun propostaConsumerFactory(): ConsumerFactory<String, ByteArray> {
		val props = mapOf(
			ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
			ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
			ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to ByteArrayDeserializer::class.java,
			// Consumer group isolado - desligar o PropostaListener nunca afeta
			// quem publica (ver CLAUDE.md, roadmap MVP 4, "Rollback").
			ConsumerConfig.GROUP_ID_CONFIG to "proposta-listener",
			ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
		)
		return DefaultKafkaConsumerFactory(props)
	}

	@Bean
	fun propostaKafkaListenerContainerFactory(
		propostaConsumerFactory: ConsumerFactory<String, ByteArray>,
		propostaKafkaTemplate: KafkaTemplate<String, ByteArray>,
	): ConcurrentKafkaListenerContainerFactory<String, ByteArray> {
		val factory = ConcurrentKafkaListenerContainerFactory<String, ByteArray>()
		factory.setConsumerFactory(propostaConsumerFactory)
		// O Circuit Breaker/Retry do Resilience4j já tentam de novo DENTRO do
		// ProcessarPropostaUsecase - se a exceção ainda assim chegar até aqui,
		// não faz sentido o container tentar de novo (FixedBackOff com 0
		// tentativas): manda direto pra DLT. Resolver explícito porque o
		// default do Spring Kafka 4.1 é "<topico>-dlt" (hífen, minúsculo) -
		// diferente de "proposta-events.DLT", o tópico já criado desde o
		// MVP 0 e documentado no README/Kafka UI da demo.
		val recoverer = DeadLetterPublishingRecoverer(propostaKafkaTemplate) { record, _ ->
			TopicPartition("${record.topic()}.DLT", record.partition())
		}
		factory.setCommonErrorHandler(DefaultErrorHandler(recoverer, FixedBackOff(0L, 0L)))
		return factory
	}
}
