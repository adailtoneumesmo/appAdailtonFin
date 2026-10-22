package br.com.meubolso.domain.despesa

import br.com.meubolso.domain.model.DespesaFixa
import br.com.meubolso.domain.model.FormaPagamento
import br.com.meubolso.domain.model.PagamentoDespesa
import br.com.meubolso.domain.model.Recorrencia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CalculadoraOcorrenciasTest {

    private val calculadora = CalculadoraOcorrencias()

    private fun despesa(
        id: Long = 1,
        dia: Int = 10,
        inicio: YearMonth = YearMonth.of(2026, 1),
        parcelas: Int? = null,
        ativa: Boolean = true,
        descricao: String = "Conta",
    ) = DespesaFixa(
        id = id,
        descricao = descricao,
        categoriaId = 1,
        valorCentavos = 10_000,
        diaVencimento = dia,
        formaPagamento = FormaPagamento.PIX,
        recorrencia = if (parcelas == null) Recorrencia.MENSAL else Recorrencia.PARCELADA,
        totalParcelas = parcelas,
        mesInicio = inicio,
        ativa = ativa,
    )

    @Test
    fun `vencimento dia 31 usa ultimo dia em meses de 28, 29 e 30 dias`() {
        val d = despesa(dia = 31)
        assertEquals(LocalDate.of(2026, 2, 28), calculadora.ocorrenciaNoMes(d, YearMonth.of(2026, 2))?.vencimento)
        assertEquals(LocalDate.of(2028, 2, 29), calculadora.ocorrenciaNoMes(d, YearMonth.of(2028, 2))?.vencimento)
        assertEquals(LocalDate.of(2026, 4, 30), calculadora.ocorrenciaNoMes(d, YearMonth.of(2026, 4))?.vencimento)
        assertEquals(LocalDate.of(2026, 10, 31), calculadora.ocorrenciaNoMes(d, YearMonth.of(2026, 10))?.vencimento)
    }

    @Test
    fun `antes do mes de inicio nao ha ocorrencia`() {
        assertNull(calculadora.ocorrenciaNoMes(despesa(inicio = YearMonth.of(2026, 11)), YearMonth.of(2026, 10)))
    }

    @Test
    fun `mensal nao tem numero de parcela`() {
        assertNull(calculadora.ocorrenciaNoMes(despesa(), YearMonth.of(2030, 5))?.parcela)
    }

    @Test
    fun `parcelada numera parcelas e encerra apos a ultima`() {
        val d = despesa(inicio = YearMonth.of(2026, 11), parcelas = 3)
        assertEquals(1, calculadora.ocorrenciaNoMes(d, YearMonth.of(2026, 11))?.parcela)
        assertEquals(2, calculadora.ocorrenciaNoMes(d, YearMonth.of(2026, 12))?.parcela)
        // virada de ano
        assertEquals(3, calculadora.ocorrenciaNoMes(d, YearMonth.of(2027, 1))?.parcela)
        assertNull(calculadora.ocorrenciaNoMes(d, YearMonth.of(2027, 2)))
    }

    @Test
    fun `pagamento do mes marca ocorrencia como paga`() {
        val d = despesa()
        val pagamentos = listOf(
            PagamentoDespesa(1, YearMonth.of(2026, 10), LocalDate.of(2026, 10, 8)),
            PagamentoDespesa(1, YearMonth.of(2026, 9), LocalDate.of(2026, 9, 9)),
        )
        val ocorrencia = calculadora.ocorrenciasNoMes(listOf(d), pagamentos, YearMonth.of(2026, 10)).single()
        assertTrue(ocorrencia.paga)
        assertEquals(LocalDate.of(2026, 10, 8), ocorrencia.dataPagamento)

        val novembro = calculadora.ocorrenciasNoMes(listOf(d), pagamentos, YearMonth.of(2026, 11)).single()
        assertFalse(novembro.paga)
    }

    @Test
    fun `inativa so aparece se ja tiver pagamento no mes`() {
        val inativa = despesa(id = 2, ativa = false)
        val mes = YearMonth.of(2026, 10)
        assertTrue(calculadora.ocorrenciasNoMes(listOf(inativa), emptyList(), mes).isEmpty())
        val pago = listOf(PagamentoDespesa(2, mes, LocalDate.of(2026, 10, 1)))
        assertEquals(1, calculadora.ocorrenciasNoMes(listOf(inativa), pago, mes).size)
    }

    @Test
    fun `vencida quando pendente e vencimento antes de hoje`() {
        val ocorrencia = calculadora.ocorrenciaNoMes(despesa(dia = 10), YearMonth.of(2026, 10))!!
        assertTrue(ocorrencia.vencida(LocalDate.of(2026, 10, 11)))
        assertFalse(ocorrencia.vencida(LocalDate.of(2026, 10, 10)))
        val paga = ocorrencia.copy(dataPagamento = LocalDate.of(2026, 10, 12))
        assertFalse(paga.vencida(LocalDate.of(2026, 10, 20)))
    }

    @Test
    fun `ocorrencias ordenadas por vencimento e descricao`() {
        val lista = calculadora.ocorrenciasNoMes(
            listOf(
                despesa(id = 1, dia = 20, descricao = "Internet"),
                despesa(id = 2, dia = 5, descricao = "Aluguel"),
                despesa(id = 3, dia = 20, descricao = "Academia"),
            ),
            emptyList(),
            YearMonth.of(2026, 10),
        )
        assertEquals(listOf("Aluguel", "Academia", "Internet"), lista.map { it.despesa.descricao })
    }
}
