package br.com.meubolso.domain.listacompra

import br.com.meubolso.domain.validacao.ErroValidacao
import java.time.LocalDate

enum class CampoListaCompra { DESCRICAO, DATA, NOME_ITEM, QUANTIDADE }

object ValidadorListaCompra {

    fun validarLista(descricao: String, data: LocalDate?): Map<CampoListaCompra, ErroValidacao> = buildMap {
        if (descricao.isBlank()) put(CampoListaCompra.DESCRICAO, ErroValidacao.OBRIGATORIO)
        if (data == null) put(CampoListaCompra.DATA, ErroValidacao.OBRIGATORIO)
    }

    /** Valor vazio vale R$ 0,00 e quantidade vazia vale 1, então só nome e formato da quantidade são checados. */
    fun validarItem(nome: String, quantidadeTexto: String): Map<CampoListaCompra, ErroValidacao> = buildMap {
        if (nome.isBlank()) put(CampoListaCompra.NOME_ITEM, ErroValidacao.OBRIGATORIO)
        if (CalculadoraListaCompra.quantidadeDeTexto(quantidadeTexto) == null) {
            put(CampoListaCompra.QUANTIDADE, ErroValidacao.QUANTIDADE_INVALIDA)
        }
    }
}
