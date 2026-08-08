package com.outboxpattern.demonstration.domain.model

/**
 * Ciclo de vida de publicação de um [OutboxEvent]. Nasce [PENDENTE]; o
 * fast-path pós-commit (`CriarPropostaUsecase`) tenta publicar imediatamente
 * e, em caso de sucesso, transiciona direto para [ENVIADO].
 *
 * Quando o fast-path não confirma (crash entre commit e publish, Kafka fora
 * do ar, etc.), o registro permanece PENDENTE até o `OutboxReprocessamentoScheduler`
 * (MVP 3) reivindicá-lo: [EM_PROCESSAMENTO] enquanto uma instância da app
 * tem o claim atômico (dono + expiração, evita duas instâncias publicarem o
 * mesmo evento); volta a PENDENTE com backoff se falhar de novo, ou vai para
 * [FALHA_DEFINITIVA] — estado terminal — quando o limite de tentativas é
 * esgotado, evitando retry infinito de uma "poison message".
 */
enum class OutboxStatusEnum {
	PENDENTE,
	EM_PROCESSAMENTO,
	ENVIADO,
	FALHA_DEFINITIVA,
}
