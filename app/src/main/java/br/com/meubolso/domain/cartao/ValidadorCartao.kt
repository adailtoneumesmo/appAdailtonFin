package br.com.meubolso.domain.cartao

import br.com.meubolso.domain.model.Origem
import br.com.meubolso.domain.validacao.ErroValidacao

enum class CampoCartao { NOME, FECHAMENTO, VENCIMENTO, FONTE }

object ValidadorCartao {

    fun validar(
        nome: String,
        diaFechamento: Int?,
        diaVencimento: Int?,
        fonte: Origem?,
    ): Map<CampoCartao, ErroValidacao> = buildMap {
        if (nome.isBlank()) put(CampoCartao.NOME, ErroValidacao.OBRIGATORIO)

        validarDia(diaFechamento)?.let { put(CampoCartao.FECHAMENTO, it) }

        validarDia(diaVencimento)?.let { put(CampoCartao.VENCIMENTO, it) }

        if (fonte == null) put(CampoCartao.FONTE, ErroValidacao.OBRIGATORIO)
    }

    private fun validarDia(dia: Int?): ErroValidacao? = when {
        dia == null -> ErroValidacao.OBRIGATORIO
        dia !in 1..31 -> ErroValidacao.DIA_FORA_DO_INTERVALO
        else -> null
    }
}
