package com.outboxpattern.demonstration.domain.port.input

import com.outboxpattern.demonstration.domain.model.Proposta
import com.outboxpattern.demonstration.domain.model.TipoAmortizacaoEnum

/**
 * Porta de entrada para criação de uma nova Proposta — contrato que a camada
 * de aplicação oferece a adapters externos (hoje: `PropostaController`), sem
 * expor detalhes de como a criação é implementada.
 */
fun interface CriarPropostaInputPort {
	fun criar(tipoAmortizacao: TipoAmortizacaoEnum): Proposta
}
