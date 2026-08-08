package com.outboxpattern.demonstration.domain.model

/**
 * Ciclo de vida da Proposta em relação ao fluxo assíncrono de eventos: toda
 * Proposta nasce [EM_ANDAMENTO] e só transiciona para [PROCESSADA] quando o
 * evento correspondente é efetivamente consumido (ver `PropostaListener`,
 * MVP 4) — ou seja, o status reflete o processamento real, não só a criação.
 */
enum class StatusPropostaEnum {
	EM_ANDAMENTO,
	PROCESSADA,
}
