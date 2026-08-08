package com.outboxpattern.demonstration.domain.model

/**
 * Ciclo de vida de publicação de um [OutboxEvent]. Nasce [PENDENTE]; o
 * fast-path pós-commit (`CriarPropostaUsecase`) tenta publicar imediatamente
 * e, em caso de sucesso, transiciona para [ENVIADO].
 *
 * MVP 2 só usa estes dois estados — o Scheduler de fallback (MVP 3) vai
 * introduzir `EM_PROCESSAMENTO` (claim atômico) e `FALHA_DEFINITIVA` (limite
 * de tentativas esgotado) quando o polling de fato existir, para não haver
 * estado no domínio sem nenhum código que o produza ainda.
 */
enum class OutboxStatusEnum {
	PENDENTE,
	ENVIADO,
}
