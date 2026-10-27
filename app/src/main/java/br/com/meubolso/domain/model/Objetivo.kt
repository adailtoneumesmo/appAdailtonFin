package br.com.meubolso.domain.model

import java.time.LocalDate

/** Meta financeira acompanhada contra o patrimônio investido (ex.: "Reserva de emergência"). */
data class Objetivo(
    val id: Long = 0,
    val nome: String,
    val valorAlvoCentavos: Long,
    /** Opcional; com ela o app calcula quanto guardar por mês. */
    val dataAlvo: LocalDate?,
)

/**
 * Quanto havia investido, no total, numa data. Hoje é informado à mão; no futuro pode vir
 * da B3 / Open Finance. Guardamos o histórico: vale sempre a posição mais recente.
 */
data class PosicaoInvestimentos(
    val id: Long = 0,
    val valorCentavos: Long,
    val dataReferencia: LocalDate,
)
