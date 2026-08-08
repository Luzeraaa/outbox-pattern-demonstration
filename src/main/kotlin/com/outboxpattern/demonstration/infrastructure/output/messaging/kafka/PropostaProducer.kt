package com.outboxpattern.demonstration.infrastructure.output.messaging.kafka

import com.outboxpattern.demonstration.domain.event.PropostaCriadaEvent
import com.outboxpattern.demonstration.domain.port.output.PropostaEventPublisherOutputPort
import com.outboxpattern.demonstration.infrastructure.output.messaging.kafka.avro.PropostaCriadaEventAvro
import org.apache.avro.io.EncoderFactory
import org.apache.avro.specific.SpecificDatumWriter
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream

/**
 * Implementa `PropostaEventPublisherOutputPort` publicando no tópico
 * "proposta-events" com serialização Avro (sem Schema Registry — ver
 * CLAUDE.md, "Stack confirmada": o schema é versionado no próprio
 * repositório). Usa `propostaId` como chave de partição, garantindo
 * ordenação de eventos do mesmo agregado.
 */
@Component
class PropostaProducer(
	private val kafkaTemplate: KafkaTemplate<String, ByteArray>,
) : PropostaEventPublisherOutputPort {

	override fun publicar(evento: PropostaCriadaEvent) {
		kafkaTemplate.send(TOPICO, evento.propostaId, serializar(evento))
	}

	/**
	 * Codifica o evento em Avro binário "puro" (sem o header de 5 bytes do
	 * formato de wire do Confluent Schema Registry, que esta POC não usa) —
	 * por isso a codificação é feita manualmente aqui, e não via um
	 * `Serializer` do Kafka configurado por classe.
	 */
	private fun serializar(evento: PropostaCriadaEvent): ByteArray {
		val registro = PropostaCriadaEventAvro.newBuilder()
			.setPropostaId(evento.propostaId)
			.setStatus(evento.status.name)
			.setTipoAmortizacao(evento.tipoAmortizacao.name)
			.setCriadaEm(evento.criadaEm.toString())
			.build()

		val saida = ByteArrayOutputStream()
		val encoder = EncoderFactory.get().binaryEncoder(saida, null)
		SpecificDatumWriter<PropostaCriadaEventAvro>(PropostaCriadaEventAvro.getClassSchema())
			.write(registro, encoder)
		encoder.flush()
		return saida.toByteArray()
	}

	companion object {
		const val TOPICO = "proposta-events"
	}
}
