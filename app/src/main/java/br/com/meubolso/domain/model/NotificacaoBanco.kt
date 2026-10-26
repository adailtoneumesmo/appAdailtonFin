package br.com.meubolso.domain.model

import java.time.Instant

/** O que o texto da notificação parece ser; o usuário decide o lançamento na confirmação. */
enum class TipoTransacao { CARTAO, PIX, ENTRADA, OUTRO }

enum class StatusNotificacao { PENDENTE, CONFIRMADA, DESCARTADA }

/** App de banco cujas notificações são lidas. */
data class BancoMonitorado(
    val nome: String,
    /** Trechos do nome do pacote (ex.: "santander" casa com "com.santander.app"). */
    val trechosPacote: List<String>,
    /** Cartão de benefício (VR/VA): a despesa sai da carteira do vale. */
    val vale: Boolean = false,
)

/** Notificação de banco capturada, aguardando o usuário lançar ou descartar. */
data class NotificacaoBanco(
    val id: Long = 0,
    val pacote: String,
    val banco: String,
    val titulo: String,
    val texto: String,
    val recebidaEm: Instant,
    /** `null` quando o valor não foi encontrado no texto. */
    val valorCentavos: Long?,
    val descricaoSugerida: String?,
    val tipo: TipoTransacao,
    val vale: Boolean,
    val status: StatusNotificacao = StatusNotificacao.PENDENTE,
)
