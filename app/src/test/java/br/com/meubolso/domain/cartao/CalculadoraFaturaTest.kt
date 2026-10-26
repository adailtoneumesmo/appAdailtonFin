package br.com.meubolso.domain.cartao

import br.com.meubolso.domain.model.Cartao
import br.com.meubolso.domain.model.CompraCartao
import br.com.meubolso.domain.model.ModalidadeCompra
import br.com.meubolso.domain.model.Origem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CalculadoraFaturaTest {

    private val calculadora = CalculadoraFatura()

    private fun cartao(fechamento: Int = 10, vencimento: Int = 17, id: Long = 1) =
        Cartao(id = id, nome = "Nubank", diaFechamento = fechamento, diaVencimento = vencimento, fonte = Origem.SALARIO)

    private fun compra(
        id: Long = 1,
        cartao: Cartao = cartao(),
        valor: Long = 10_000,
        data: LocalDate = LocalDate.of(2026, 3, 5),
        modalidade: ModalidadeCompra = ModalidadeCompra.A_VISTA,
        parcelas: Int? = null,
        ultima: YearMonth? = null,
        descricao: String = "Compra",
    ) = CompraCartao(
        id = id,
        cartaoId = cartao.id,
        descricao = descricao,
        categoriaId = 1,
        valorCentavos = valor,
        dataCompra = data,
        modalidade = modalidade,
        totalParcelas = parcelas,
        mesPrimeiraFatura = calculadora.faturaDaCompra(cartao, data),
        mesUltimaFatura = ultima,
    )

    private val marco = YearMonth.of(2026, 3)

    // Em qual fatura a compra entra

    @Test
    fun `compra antes do fechamento entra na fatura do mes`() {
        assertEquals(marco, calculadora.faturaDaCompra(cartao(fechamento = 10), LocalDate.of(2026, 3, 5)))
        assertEquals(marco, calculadora.faturaDaCompra(cartao(fechamento = 10), LocalDate.of(2026, 3, 9)))
    }

    @Test
    fun `compra no dia do fechamento ou depois entra na fatura seguinte`() {
        val abril = YearMonth.of(2026, 4)
        assertEquals(abril, calculadora.faturaDaCompra(cartao(fechamento = 10), LocalDate.of(2026, 3, 10)))
        assertEquals(abril, calculadora.faturaDaCompra(cartao(fechamento = 10), LocalDate.of(2026, 3, 15)))
    }

    @Test
    fun `compra depois do fechamento de dezembro vira o ano`() {
        assertEquals(YearMonth.of(2027, 1), calculadora.faturaDaCompra(cartao(), LocalDate.of(2026, 12, 20)))
    }

    @Test
    fun `fechamento dia 31 usa o ultimo dia do mes`() {
        val c = cartao(fechamento = 31, vencimento = 10)
        assertEquals(LocalDate.of(2026, 2, 28), calculadora.fechamento(c, YearMonth.of(2026, 2)))
        assertEquals(LocalDate.of(2028, 2, 29), calculadora.fechamento(c, YearMonth.of(2028, 2)))
        assertEquals(LocalDate.of(2026, 4, 30), calculadora.fechamento(c, YearMonth.of(2026, 4)))
        // 27/02 ainda é antes do fechamento ajustado; 28/02 já é o próprio fechamento.
        assertEquals(YearMonth.of(2026, 2), calculadora.faturaDaCompra(c, LocalDate.of(2026, 2, 27)))
        assertEquals(YearMonth.of(2026, 3), calculadora.faturaDaCompra(c, LocalDate.of(2026, 2, 28)))
    }

    @Test
    fun `vencimento depois do fechamento fica no mesmo mes`() {
        assertEquals(LocalDate.of(2026, 3, 17), calculadora.vencimento(cartao(fechamento = 10, vencimento = 17), marco))
    }

    @Test
    fun `vencimento antes ou no dia do fechamento fica no mes seguinte`() {
        assertEquals(LocalDate.of(2026, 4, 5), calculadora.vencimento(cartao(fechamento = 28, vencimento = 5), marco))
        assertEquals(LocalDate.of(2026, 4, 10), calculadora.vencimento(cartao(fechamento = 10, vencimento = 10), marco))
    }

    @Test
    fun `vencimento dia 31 em mes curto usa o ultimo dia`() {
        val c = cartao(fechamento = 20, vencimento = 31)
        assertEquals(LocalDate.of(2026, 2, 28), calculadora.vencimento(c, YearMonth.of(2026, 2)))
        // Fecha 31/01 e vence "dia 31" de fevereiro, que vira 28/02.
        val c2 = cartao(fechamento = 31, vencimento = 31)
        assertEquals(LocalDate.of(2026, 2, 28), calculadora.vencimento(c2, YearMonth.of(2026, 1)))
    }

    // Parcelamento

    @Test
    fun `divisao exata reparte igualmente`() {
        assertEquals(List(5) { 10_000L }, calculadora.dividirEmParcelas(50_000, 5))
    }

    @Test
    fun `diferenca de centavos fica na ultima parcela`() {
        assertEquals(listOf(3_333L, 3_333L, 3_334L), calculadora.dividirEmParcelas(10_000, 3))
        assertEquals(listOf(1L, 1L, 2L), calculadora.dividirEmParcelas(4, 3))
    }

    @Test
    fun `parcelas sempre somam o total`() {
        for (total in listOf(1_99L, 10_001L, 123_457L)) {
            for (n in 2..12) {
                assertEquals("total=$total n=$n", total, calculadora.dividirEmParcelas(total, n).sum())
            }
        }
    }

    @Test
    fun `compra de 500 em 5x vai de marco a julho numerada de 1 a 5`() {
        val c = compra(valor = 50_000, modalidade = ModalidadeCompra.PARCELADO, parcelas = 5)
        val lancamentos = (0L..4L).map { calculadora.lancamentoNaFatura(c, marco.plusMonths(it)) }
        assertEquals(List(5) { 10_000L }, lancamentos.map { it?.valorCentavos })
        assertEquals(listOf(1, 2, 3, 4, 5), lancamentos.map { it?.parcela })
        assertEquals(listOf(4, 3, 2, 1, 0), lancamentos.map { it?.parcelasRestantes })
        assertNull(calculadora.lancamentoNaFatura(c, YearMonth.of(2026, 2)))
        assertNull(calculadora.lancamentoNaFatura(c, YearMonth.of(2026, 8)))
    }

    @Test
    fun `parcelada comprada depois do fechamento comeca na fatura seguinte`() {
        val c = compra(data = LocalDate.of(2026, 3, 15), valor = 10_000, modalidade = ModalidadeCompra.PARCELADO, parcelas = 3)
        assertNull(calculadora.lancamentoNaFatura(c, marco))
        assertEquals(1, calculadora.lancamentoNaFatura(c, YearMonth.of(2026, 4))?.parcela)
        assertEquals(3_334L, calculadora.lancamentoNaFatura(c, YearMonth.of(2026, 6))?.valorCentavos)
        assertNull(calculadora.lancamentoNaFatura(c, YearMonth.of(2026, 7)))
    }

    // À vista e fixo

    @Test
    fun `a vista entra so em uma fatura`() {
        val c = compra()
        assertEquals(10_000L, calculadora.lancamentoNaFatura(c, marco)?.valorCentavos)
        assertNull(calculadora.lancamentoNaFatura(c, marco)?.parcela)
        assertNull(calculadora.lancamentoNaFatura(c, YearMonth.of(2026, 4)))
        assertNull(calculadora.lancamentoNaFatura(c, YearMonth.of(2026, 2)))
    }

    @Test
    fun `fixa aparece em todas as faturas a partir da primeira`() {
        val netflix = compra(valor = 3_990, modalidade = ModalidadeCompra.FIXO)
        assertNull(calculadora.lancamentoNaFatura(netflix, YearMonth.of(2026, 2)))
        for (mes in listOf(marco, YearMonth.of(2026, 9), YearMonth.of(2030, 1))) {
            assertEquals(3_990L, calculadora.lancamentoNaFatura(netflix, mes)?.valorCentavos)
        }
    }

    @Test
    fun `fixa encerrada mantem o historico e some depois da ultima fatura`() {
        val netflix = compra(valor = 3_990, modalidade = ModalidadeCompra.FIXO, ultima = YearMonth.of(2026, 6))
        assertEquals(3_990L, calculadora.lancamentoNaFatura(netflix, marco)?.valorCentavos)
        assertEquals(3_990L, calculadora.lancamentoNaFatura(netflix, YearMonth.of(2026, 6))?.valorCentavos)
        assertNull(calculadora.lancamentoNaFatura(netflix, YearMonth.of(2026, 7)))
    }

    // Fatura completa

    @Test
    fun `total da fatura e a soma exata dos lancamentos`() {
        val c = cartao()
        val compras = listOf(
            compra(id = 1, valor = 50_000, modalidade = ModalidadeCompra.PARCELADO, parcelas = 5),
            compra(id = 2, valor = 10_000, modalidade = ModalidadeCompra.PARCELADO, parcelas = 3),
            compra(id = 3, valor = 3_990, modalidade = ModalidadeCompra.FIXO),
            compra(id = 4, valor = 2_550),
        )
        val maio = calculadora.fatura(c, compras, YearMonth.of(2026, 5))
        assertEquals(listOf(10_000L, 3_334L, 3_990L), maio.lancamentos.map { it.valorCentavos })
        assertEquals(17_324L, maio.totalCentavos)

        val marcoFatura = calculadora.fatura(c, compras, marco)
        assertEquals(10_000L + 3_333L + 3_990L + 2_550L, marcoFatura.totalCentavos)
        assertEquals(marcoFatura.lancamentos.sumOf { it.valorCentavos }, marcoFatura.totalCentavos)
    }

    @Test
    fun `editar ou excluir a compra recalcula todas as faturas`() {
        val c = cartao()
        val original = compra(valor = 50_000, modalidade = ModalidadeCompra.PARCELADO, parcelas = 5)
        val editada = original.copy(valorCentavos = 60_000, totalParcelas = 4)
        val abril = YearMonth.of(2026, 4)
        val julho = YearMonth.of(2026, 7)

        assertEquals(10_000L, calculadora.fatura(c, listOf(original), abril).totalCentavos)
        assertEquals(15_000L, calculadora.fatura(c, listOf(editada), abril).totalCentavos)
        assertEquals(0L, calculadora.fatura(c, listOf(editada), julho).totalCentavos)
        assertEquals(0L, calculadora.fatura(c, emptyList(), abril).totalCentavos)
    }

    @Test
    fun `fatura ignora compras de outros cartoes e ordena por data`() {
        val c = cartao(id = 1)
        val outro = cartao(id = 2)
        val compras = listOf(
            compra(id = 1, data = LocalDate.of(2026, 3, 8), descricao = "Mercado"),
            compra(id = 2, data = LocalDate.of(2026, 3, 2), descricao = "Farmácia"),
            compra(id = 3, cartao = outro, data = LocalDate.of(2026, 3, 1), descricao = "Outro cartão"),
        )
        val fatura = calculadora.fatura(c, compras, marco)
        assertEquals(listOf("Farmácia", "Mercado"), fatura.lancamentos.map { it.compra.descricao })
        assertEquals(LocalDate.of(2026, 3, 10), fatura.fechamento)
        assertEquals(LocalDate.of(2026, 3, 17), fatura.vencimento)
    }

    // Estorno

    @Test
    fun `estorno abate o total da fatura`() {
        val c = cartao()
        val compras = listOf(
            compra(id = 1, valor = 12_000),
            compra(id = 2, valor = -50, data = LocalDate.of(2026, 3, 7), descricao = "Estorno"),
        )
        val fatura = calculadora.fatura(c, compras, marco)
        assertEquals(listOf(12_000L, -50L), fatura.lancamentos.map { it.valorCentavos })
        assertEquals(11_950L, fatura.totalCentavos)
        assertTrue(fatura.lancamentos.last().compra.estorno)
    }

    @Test
    fun `estorno segue a mesma regra de fechamento`() {
        val estorno = compra(valor = -50, data = LocalDate.of(2026, 3, 12))
        assertNull(calculadora.lancamentoNaFatura(estorno, marco))
        assertEquals(-50L, calculadora.lancamentoNaFatura(estorno, YearMonth.of(2026, 4))?.valorCentavos)
    }

    @Test
    fun `estorno parcelado divide o credito com a diferenca na ultima`() {
        assertEquals(listOf(-3_333L, -3_333L, -3_334L), calculadora.dividirEmParcelas(-10_000, 3))
        val estorno = compra(valor = -10_000, modalidade = ModalidadeCompra.PARCELADO, parcelas = 3)
        assertEquals(-3_334L, calculadora.lancamentoNaFatura(estorno, YearMonth.of(2026, 5))?.valorCentavos)
    }

    @Test
    fun `fatura fecha no dia do fechamento`() {
        val fatura = calculadora.fatura(cartao(), emptyList(), marco)
        assertFalse(fatura.fechada(LocalDate.of(2026, 3, 9)))
        assertTrue(fatura.fechada(LocalDate.of(2026, 3, 10)))
    }
}
