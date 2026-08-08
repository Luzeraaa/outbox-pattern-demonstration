package com.outboxpattern.demonstration.infrastructure.input.kafka

import com.outboxpattern.demonstration.domain.port.input.ProcessarPropostaInputPort
import com.outboxpattern.demonstration.infrastructure.output.messaging.kafka.PropostaProducer
import com.outboxpattern.demonstration.infrastructure.output.messaging.kafka.avro.PropostaCriadaEventAvro
import org.apache.avro.io.DecoderFactory
import org.apache.avro.specific.SpecificDatumReader
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

/**
 * Adapter de entrada Kafka: consome `proposta-events` e aciona
 * `ProcessarPropostaInputPort`. Consumer group isolado (`proposta-listener`)
 * — desabilitar este listener (`proposta.listener.enabled=false`) nunca
 * afeta a publicação (fast-path/Scheduler seguem funcionando normalmente).
 *
 * Se a desserialização Avro falhar (payload não é um `PropostaCriadaEventAvro`
 * válido) ou `processar` propagar uma exceção mesmo depois do Circuit
 * Breaker/Retry (ver `ProcessarPropostaUsecase`), quem decide o destino final
 * é o error handler do container (`KafkaConfig`), que manda a mensagem
 * original para `proposta-events.DLT`.
 */
@Component
@ConditionalOnProperty(prefix = "proposta.listener", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class PropostaListener(
	private val processarPropostaInputPort: ProcessarPropostaInputPort,
) {

	@KafkaListener(
		topics = [PropostaProducer.TOPICO],
		groupId = "proposta-listener",
		containerFactory = "propostaKafkaListenerContainerFactory",
	)
	fun ouvir(payload: ByteArray) {
		val evento = desserializar(payload)
		processarPropostaInputPort.processar(evento.propostaId.toString())
	}

	/** Mesmo formato binário "puro" (sem header de wire do Schema Registry)
	 * usado pelo `PropostaProducer` para escrever — leitura simétrica. */
	private fun desserializar(payload: ByteArray): PropostaCriadaEventAvro {
		val decoder = DecoderFactory.get().binaryDecoder(payload, null)
		return SpecificDatumReader(PropostaCriadaEventAvro::class.java).read(null, decoder)
	}
}
