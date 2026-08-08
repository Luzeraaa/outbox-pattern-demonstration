package com.outboxpattern.demonstration.infrastructure.input.rest.dto

import com.outboxpattern.demonstration.domain.model.Proposta
import com.outboxpattern.demonstration.domain.model.StatusPropostaEnum
import com.outboxpattern.demonstration.domain.model.TipoAmortizacaoEnum
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

/**
 * Representação da Proposta retornada pela API. Nunca expõe o domínio direto
 * — só o que a liderança precisa ver na demo (id, status, tipo, data).
 */
@Schema(description = "Representação da Proposta persistida")
data class PropostaResponseDto(
	val id: String,
	val status: StatusPropostaEnum,
	val tipoAmortizacao: TipoAmortizacaoEnum,
	val criadaEm: Instant,
) {
	companion object {
		/**
		 * Só existe conversão a partir de uma Proposta já persistida (com id
		 * preenchido) — expor uma Proposta ainda não salva pela API não faz
		 * sentido de negócio nesta POC.
		 */
		fun deDominio(proposta: Proposta): PropostaResponseDto =
			PropostaResponseDto(
				id = requireNotNull(proposta.id) { "Proposta retornada pela API precisa estar persistida" },
				status = proposta.status,
				tipoAmortizacao = proposta.tipoAmortizacao,
				criadaEm = proposta.criadaEm,
			)
	}
}
