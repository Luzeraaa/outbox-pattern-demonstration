package com.outboxpattern.demonstration.domain.port.input

/**
 * Porta de entrada para o processamento de uma Proposta a partir do evento
 * consumido do Kafka — contrato que a aplicação oferece ao `PropostaListener`.
 */
fun interface ProcessarPropostaInputPort {
	fun processar(propostaId: String)
}
