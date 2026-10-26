package br.com.meubolso.domain.model

import java.time.LocalDate

/** [VALE] é o adiantamento salarial; [VALE_ALIMENTACAO] é o VR/VA, uma carteira à parte. */
enum class TipoRenda { SALARIO, VALE, RENDA_EXTRA, VALE_ALIMENTACAO }

/** Como o valor mensal do Vale Refeição/Alimentação é obtido. */
enum class FormaCalculoVale {
    /** [Renda.valorCentavos] é o valor do mês. */
    VALOR_FIXO,

    /** [Renda.valorCentavos] é o valor diário, multiplicado pelos dias úteis do mês seguinte. */
    VALOR_POR_DIA,
}

/** Como o dia de recebimento é interpretado. */
enum class ModoDiaRecebimento {
    /** Dia fixo do mês (1–31); em meses mais curtos vale o último dia. */
    DIA_FIXO,

    /** N-ésimo dia útil do mês (ex.: 5º dia útil). */
    DIA_UTIL,
}

data class Renda(
    val id: Long = 0,
    val descricao: String,
    val tipo: TipoRenda,
    val valorCentavos: Long,
    val modoDia: ModoDiaRecebimento,
    val dia: Int,
    val ativo: Boolean = true,
    val dataInicio: LocalDate,
    val dataFim: LocalDate? = null,
    /** Só para [TipoRenda.VALE_ALIMENTACAO]. */
    val formaCalculoVale: FormaCalculoVale? = null,
) {
    fun vigenteEm(data: LocalDate): Boolean =
        !data.isBefore(dataInicio) && (dataFim == null || !data.isAfter(dataFim))
}
