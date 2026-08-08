package com.outboxpattern.demonstration.infrastructure.input.rest

import com.outboxpattern.demonstration.domain.port.input.CriarPropostaInputPort
import com.outboxpattern.demonstration.infrastructure.input.rest.dto.PropostaRequestDto
import com.outboxpattern.demonstration.infrastructure.input.rest.dto.PropostaResponseDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Adapter de entrada HTTP para o domínio de Proposta. Depende só da porta de
 * entrada (`CriarPropostaInputPort`) — nunca do usecase ou da persistência
 * diretamente, para que a regra de dependência da arquitetura hexagonal
 * (infra → aplicação → domínio) não seja violada.
 */
@RestController
@RequestMapping("/api/propostas")
@Tag(name = "Propostas", description = "Criação de propostas de crédito (fluxo Outbox Pattern)")
class PropostaController(
	private val criarPropostaInputPort: CriarPropostaInputPort,
) {

	@PostMapping
	@Operation(
		summary = "Cria uma nova Proposta",
		description = "A Proposta nasce sempre com status EM_ANDAMENTO. A partir do MVP 2, " +
			"esta chamada também registra o OutboxEvent que dispara a publicação no Kafka.",
	)
	fun criar(@Valid @RequestBody request: PropostaRequestDto): ResponseEntity<PropostaResponseDto> {
		val proposta = criarPropostaInputPort.criar(request.tipoAmortizacao)
		return ResponseEntity.status(HttpStatus.CREATED).body(PropostaResponseDto.deDominio(proposta))
	}
}
