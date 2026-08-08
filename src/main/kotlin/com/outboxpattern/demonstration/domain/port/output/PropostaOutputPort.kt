package com.outboxpattern.demonstration.domain.port.output

import com.outboxpattern.demonstration.domain.model.Proposta

/**
 * Porta de saída para persistência de Proposta — contrato que a infraestrutura
 * precisa cumprir (hoje: `PropostaRepositoryAdapter`, via Spring Data
 * MongoDB). O domínio/aplicação depende só desta interface, nunca do driver.
 */
interface PropostaOutputPort {
	fun salvar(proposta: Proposta): Proposta

	/** Usado pelo `ProcessarPropostaUsecase` (MVP 4) para checar o status
	 * atual antes de transicionar — é a base da idempotência do Listener. */
	fun buscarPorId(id: String): Proposta?
}
