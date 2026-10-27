package br.com.meubolso.domain.objetivo

import br.com.meubolso.domain.validacao.ErroValidacao

enum class CampoObjetivo { NOME, VALOR_ALVO }

object ValidadorObjetivo {

    fun validar(nome: String, valorAlvoCentavos: Long): Map<CampoObjetivo, ErroValidacao> = buildMap {
        if (nome.isBlank()) put(CampoObjetivo.NOME, ErroValidacao.OBRIGATORIO)
        if (valorAlvoCentavos <= 0) put(CampoObjetivo.VALOR_ALVO, ErroValidacao.VALOR_NAO_POSITIVO)
    }
}
