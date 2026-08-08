package com.outboxpattern.demonstration.domain.port.output

import com.outboxpattern.demonstration.domain.event.PropostaCriadaEvent

/**
 * Porta de saída para publicação de eventos de Proposta — contrato que a
 * infraestrutura de mensageria precisa cumprir (hoje: `PropostaProducer`,
 * via Kafka + Avro). A aplicação depende só desta porta, nunca do
 * `KafkaTemplate` diretamente.
 */
fun interface PropostaEventPublisherOutputPort {
	fun publicar(evento: PropostaCriadaEvent)
}
