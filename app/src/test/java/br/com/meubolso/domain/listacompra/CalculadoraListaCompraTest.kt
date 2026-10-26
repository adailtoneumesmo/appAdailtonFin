package br.com.meubolso.domain.listacompra

import br.com.meubolso.domain.model.ItemListaCompra
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculadoraListaCompraTest {

    private fun item(valor: Long, qtd: Long = 1_000, noCarrinho: Boolean = false) =
        ItemListaCompra(listaId = 1, nome = "Item", valorCentavos = valor, quantidadeMilesimos = qtd, noCarrinho = noCarrinho)

    @Test
    fun `total do item e valor vezes quantidade`() {
        assertEquals(1_798L, CalculadoraListaCompra.totalItem(899, 2_000))
        assertEquals(0L, CalculadoraListaCompra.totalItem(0, 3_000))
    }

    @Test
    fun `quantidade fracionada arredonda ao centavo`() {
        // R$ 12,99/kg × 1,5 kg = R$ 19,485 → R$ 19,49
        assertEquals(1_949L, CalculadoraListaCompra.totalItem(1_299, 1_500))
        // R$ 3,33 × 0,333 = R$ 1,10889 → R$ 1,11
        assertEquals(111L, CalculadoraListaCompra.totalItem(333, 333))
    }

    @Test
    fun `resumo separa total da lista e preco atual do carrinho`() {
        val resumo = CalculadoraListaCompra.resumir(
            listOf(item(1_000, 2_000, noCarrinho = true), item(500), item(250, 4_000, noCarrinho = true)),
        )
        assertEquals(3, resumo.quantidadeItens)
        assertEquals(2, resumo.itensNoCarrinho)
        assertEquals(1, resumo.itensDesmarcados)
        assertEquals(3_500L, resumo.totalCentavos)
        assertEquals(3_000L, resumo.carrinhoCentavos)
        assertNull(resumo.restanteCentavos)
        assertFalse(resumo.acimaDoLimite)
    }

    @Test
    fun `ainda tem para gastar e limite menos total da lista`() {
        val resumo = CalculadoraListaCompra.resumir(listOf(item(10_000), item(5_000, noCarrinho = true)), limiteCentavos = 38_000)
        assertEquals(23_000L, resumo.restanteCentavos)
        assertFalse(resumo.acimaDoLimite)
    }

    @Test
    fun `acima do limite fica negativo`() {
        val resumo = CalculadoraListaCompra.resumir(listOf(item(40_000)), limiteCentavos = 38_000)
        assertEquals(-2_000L, resumo.restanteCentavos)
        assertTrue(resumo.acimaDoLimite)
    }

    @Test
    fun `lista vazia tem totais zerados`() {
        val resumo = CalculadoraListaCompra.resumir(emptyList(), limiteCentavos = 10_000)
        assertEquals(0L, resumo.totalCentavos)
        assertEquals(10_000L, resumo.restanteCentavos)
    }

    @Test
    fun `quantidade em texto aceita virgula e vazio vale um`() {
        assertEquals(1_500L, CalculadoraListaCompra.quantidadeDeTexto("1,5"))
        assertEquals(1_500L, CalculadoraListaCompra.quantidadeDeTexto("1.5"))
        assertEquals(1_000L, CalculadoraListaCompra.quantidadeDeTexto(""))
        assertEquals(250L, CalculadoraListaCompra.quantidadeDeTexto("0,25"))
        assertNull(CalculadoraListaCompra.quantidadeDeTexto("abc"))
        assertNull(CalculadoraListaCompra.quantidadeDeTexto("1,2345"))
        assertNull(CalculadoraListaCompra.quantidadeDeTexto("-1"))
    }

    @Test
    fun `quantidade para texto remove zeros a direita`() {
        assertEquals("1,5", CalculadoraListaCompra.quantidadeParaTexto(1_500))
        assertEquals("2", CalculadoraListaCompra.quantidadeParaTexto(2_000))
        assertEquals("0,25", CalculadoraListaCompra.quantidadeParaTexto(250))
    }
}
