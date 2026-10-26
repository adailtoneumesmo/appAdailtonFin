package br.com.meubolso.domain.renda

import br.com.meubolso.domain.model.FormaCalculoVale
import br.com.meubolso.domain.model.ModoDiaRecebimento
import br.com.meubolso.domain.model.TipoRenda
import br.com.meubolso.domain.validacao.ErroValidacao
import java.time.LocalDate

enum class CampoRenda { DESCRICAO, VALOR, DIA, DATA_INICIO, DATA_FIM, FORMA_CALCULO }

object ValidadorRenda {

    /** Um mês tem no máximo 23 dias úteis. */
    const val MAX_DIA_UTIL = 23

    fun validar(
        descricao: String,
        valorCentavos: Long,
        modoDia: ModoDiaRecebimento,
        dia: Int?,
        dataInicio: LocalDate?,
        dataFim: LocalDate?,
        tipo: TipoRenda = TipoRenda.SALARIO,
        formaCalculoVale: FormaCalculoVale? = null,
    ): Map<CampoRenda, ErroValidacao> = buildMap {
        if (descricao.isBlank()) put(CampoRenda.DESCRICAO, ErroValidacao.OBRIGATORIO)

        if (valorCentavos <= 0) put(CampoRenda.VALOR, ErroValidacao.VALOR_NAO_POSITIVO)

        if (tipo == TipoRenda.VALE_ALIMENTACAO && formaCalculoVale == null) {
            put(CampoRenda.FORMA_CALCULO, ErroValidacao.OBRIGATORIO)
        }

        when {
            dia == null -> put(CampoRenda.DIA, ErroValidacao.OBRIGATORIO)
            modoDia == ModoDiaRecebimento.DIA_FIXO && dia !in 1..31 ->
                put(CampoRenda.DIA, ErroValidacao.DIA_FORA_DO_INTERVALO)
            modoDia == ModoDiaRecebimento.DIA_UTIL && dia !in 1..MAX_DIA_UTIL ->
                put(CampoRenda.DIA, ErroValidacao.DIA_UTIL_FORA_DO_INTERVALO)
        }

        if (dataInicio == null) {
            put(CampoRenda.DATA_INICIO, ErroValidacao.OBRIGATORIO)
        } else if (dataFim != null && dataFim.isBefore(dataInicio)) {
            put(CampoRenda.DATA_FIM, ErroValidacao.DATA_FIM_ANTES_DO_INICIO)
        }
    }
}
