package com.outboxpattern.demonstration.domain.exception

/**
 * Lançada quando o `PropostaListener` recebe um evento `PropostaCriada` cujo
 * `propostaId` não existe no Mongo. Cenário anômalo (mensagem manual de
 * teste, corrupção de dado), diferente de uma duplicata legítima — duplicata
 * é idempotência tratada normalmente (no-op), não exceção. Propaga pelo
 * Circuit Breaker/Retry (`ProcessarPropostaUsecase`) e, se esgotados,
 * termina na DLT.
 */
class PropostaNaoEncontradaException(propostaId: String) :
	RuntimeException("Proposta $propostaId não encontrada para processar o evento PropostaCriada")
