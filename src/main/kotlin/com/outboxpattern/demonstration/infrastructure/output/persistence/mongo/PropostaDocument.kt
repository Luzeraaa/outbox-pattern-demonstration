package com.outboxpattern.demonstration.infrastructure.output.persistence.mongo

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

/**
 * Modelo espelho de persistência da Proposta, coleção "propostas". Existe
 * separado do domínio para que `Proposta` (domain/model) permaneça Kotlin
 * puro, sem anotação de Spring Data MongoDB — a conversão é feita só pelo
 * `PropostaMapper`.
 */
@Document(collection = "propostas")
data class PropostaDocument(
	@Id
	val id: String? = null,
	val status: String,
	val tipoAmortizacao: String,
	val criadaEm: Instant,
)
