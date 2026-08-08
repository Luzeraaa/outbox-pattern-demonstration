package com.outboxpattern.demonstration.domain.model

/**
 * Sistema de amortização escolhido para a Proposta de crédito: [SAC]
 * (amortização constante, parcelas decrescentes) ou [PRICE] (parcelas fixas,
 * juros decrescentes). Esta POC não calcula o plano de amortização — o campo
 * existe para ilustrar um atributo real do domínio de crédito.
 */
enum class TipoAmortizacaoEnum {
	SAC,
	PRICE,
}
