package com.outboxpattern.demonstration.infrastructure.input.rest.dto

import com.outboxpattern.demonstration.domain.model.TipoAmortizacaoEnum
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

/**
 * Corpo de entrada do POST de criação de Proposta. Nunca expõe o domínio
 * direto — a REST só enxerga este contrato, documentado no Swagger para a
 * liderança usar via "Try it out".
 */
@Schema(description = "Dados necessários para criar uma nova Proposta de crédito")
data class PropostaRequestDto(
	@field:NotNull
	@Schema(description = "Sistema de amortização escolhido", example = "SAC")
	val tipoAmortizacao: TipoAmortizacaoEnum,
)
