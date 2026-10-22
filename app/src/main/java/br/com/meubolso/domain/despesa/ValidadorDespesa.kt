package br.com.meubolso.domain.despesa

import br.com.meubolso.domain.model.Recorrencia
import br.com.meubolso.domain.validacao.ErroValidacao

enum class CampoDespesa { DESCRICAO, CATEGORIA, VALOR, DIA, PARCELAS }

object ValidadorDespesa {

    val INTERVALO_PARCELAS = 2..360

    fun validar(
        descricao: String,
        categoriaId: Long?,
        valorCentavos: Long,
        diaVencimento: Int?,
        recorrencia: Recorrencia,
        totalParcelas: Int?,
    ): Map<CampoDespesa, ErroValidacao> = buildMap {
        if (descricao.isBlank()) put(CampoDespesa.DESCRICAO, ErroValidacao.OBRIGATORIO)

        if (categoriaId == null) put(CampoDespesa.CATEGORIA, ErroValidacao.OBRIGATORIO)

        if (valorCentavos <= 0) put(CampoDespesa.VALOR, ErroValidacao.VALOR_NAO_POSITIVO)

        when {
            diaVencimento == null -> put(CampoDespesa.DIA, ErroValidacao.OBRIGATORIO)
            diaVencimento !in 1..31 -> put(CampoDespesa.DIA, ErroValidacao.DIA_FORA_DO_INTERVALO)
        }

        if (recorrencia == Recorrencia.PARCELADA) {
            when {
                totalParcelas == null -> put(CampoDespesa.PARCELAS, ErroValidacao.OBRIGATORIO)
                totalParcelas !in INTERVALO_PARCELAS ->
                    put(CampoDespesa.PARCELAS, ErroValidacao.PARCELAS_FORA_DO_INTERVALO)
            }
        }
    }
}
