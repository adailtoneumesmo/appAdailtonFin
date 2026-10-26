package br.com.meubolso.domain.listacompra

import br.com.meubolso.domain.model.ItemListaCompra
import java.math.BigDecimal
import java.math.RoundingMode

/** Totais de uma lista de compras, recalculados a cada alteração de item. */
data class ResumoListaCompra(
    val quantidadeItens: Int = 0,
    val itensNoCarrinho: Int = 0,
    /** Soma de todos os itens. */
    val totalCentavos: Long = 0,
    /** Soma só dos itens marcados no carrinho. */
    val carrinhoCentavos: Long = 0,
    val limiteCentavos: Long? = null,
) {
    val itensDesmarcados: Int get() = quantidadeItens - itensNoCarrinho

    /** Limite − total da lista; `null` sem limite. Negativo quando ultrapassa. */
    val restanteCentavos: Long? get() = limiteCentavos?.let { it - totalCentavos }

    val acimaDoLimite: Boolean get() = (restanteCentavos ?: 0) < 0
}

object CalculadoraListaCompra {

    private const val ESCALA_QUANTIDADE = 3

    /** Valor unitário × quantidade, arredondado ao centavo (meio para cima). */
    fun totalItem(valorCentavos: Long, quantidadeMilesimos: Long): Long =
        BigDecimal.valueOf(valorCentavos)
            .multiply(BigDecimal.valueOf(quantidadeMilesimos, ESCALA_QUANTIDADE))
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()

    fun totalItem(item: ItemListaCompra): Long = totalItem(item.valorCentavos, item.quantidadeMilesimos)

    fun resumir(itens: List<ItemListaCompra>, limiteCentavos: Long? = null): ResumoListaCompra = ResumoListaCompra(
        quantidadeItens = itens.size,
        itensNoCarrinho = itens.count { it.noCarrinho },
        totalCentavos = itens.sumOf(::totalItem),
        carrinhoCentavos = itens.filter { it.noCarrinho }.sumOf(::totalItem),
        limiteCentavos = limiteCentavos,
    )

    /**
     * Texto digitado → milésimos. Aceita vírgula ou ponto ("1,5" → 1500). Vazio = 1.
     * Retorna `null` se não for um número válido ou se tiver mais de 3 casas decimais.
     */
    fun quantidadeDeTexto(texto: String): Long? {
        val limpo = texto.trim().replace(',', '.')
        if (limpo.isEmpty()) return ItemListaCompra.QUANTIDADE_PADRAO
        val numero = limpo.toBigDecimalOrNull()?.takeIf { it.signum() >= 0 } ?: return null
        if (numero.stripTrailingZeros().scale() > ESCALA_QUANTIDADE) return null
        return numero.movePointRight(ESCALA_QUANTIDADE).longValueExact()
    }

    /** 1500 → "1,5"; 2000 → "2". */
    fun quantidadeParaTexto(milesimos: Long): String =
        BigDecimal.valueOf(milesimos, ESCALA_QUANTIDADE).stripTrailingZeros().toPlainString().replace('.', ',')
}
