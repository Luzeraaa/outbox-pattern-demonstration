package com.outboxpattern.demonstration.domain.port.input

/**
 * Porta de entrada para o fallback de reprocessamento do outbox — contrato
 * que a aplicação oferece ao `OutboxReprocessamentoScheduler`. Não é
 * acionada por um usuário como as demais InputPort, mas segue o mesmo
 * princípio: a infraestrutura de entrada (aqui, o Scheduler) nunca orquestra
 * domínio/output ports diretamente, só chama através desta porta.
 */
fun interface ReprocessarOutboxInputPort {
	/**
	 * Reivindica e tenta republicar todo o lote elegível nesta rodada.
	 * @param dono identifica a instância da app que está reivindicando os
	 * registros — usado no claim atômico para evitar publicação duplicada.
	 * @return quantidade de OutboxEvents processados (sucesso ou falha
	 * definitiva); usado só para log/observabilidade.
	 */
	fun reprocessarPendentes(dono: String): Int
}
