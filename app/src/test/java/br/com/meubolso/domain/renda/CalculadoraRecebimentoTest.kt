package br.com.meubolso.domain.renda

import br.com.meubolso.domain.calendario.CalendarioDiasUteis
import br.com.meubolso.domain.model.ModoDiaRecebimento
import br.com.meubolso.domain.model.Renda
import br.com.meubolso.domain.model.TipoRenda
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CalculadoraRecebimentoTest {

    private val calculadora = CalculadoraRecebimento(CalendarioDiasUteis())

    private fun renda(
        dia: Int,
        modo: ModoDiaRecebimento = ModoDiaRecebimento.DIA_FIXO,
        inicio: LocalDate = LocalDate.of(2020, 1, 1),
        fim: LocalDate? = null,
    ) = Renda(
        descricao = "Teste",
        tipo = TipoRenda.SALARIO,
        valorCentavos = 100_000,
        modoDia = modo,
        dia = dia,
        dataInicio = inicio,
        dataFim = fim,
    )

    @Test
    fun `dia 31 em mes de 30 dias usa o ultimo dia`() {
        val data = calculadora.dataNoMes(renda(31), YearMonth.of(2026, 6), anteciparDiaNaoUtil = false)
        assertEquals(LocalDate.of(2026, 6, 30), data?.prevista)
    }

    @Test
    fun `dia 30 em fevereiro usa 28 ou 29`() {
        assertEquals(
            LocalDate.of(2026, 2, 28),
            calculadora.dataNoMes(renda(30), YearMonth.of(2026, 2), false)?.nominal,
        )
        assertEquals(
            LocalDate.of(2028, 2, 29),
            calculadora.dataNoMes(renda(30), YearMonth.of(2028, 2), false)?.nominal,
        )
    }

    @Test
    fun `dia em sabado e antecipado para sexta quando regra ligada`() {
        val data = calculadora.dataNoMes(renda(10), YearMonth.of(2026, 10), anteciparDiaNaoUtil = true)!!
        assertEquals(LocalDate.of(2026, 10, 10), data.nominal)
        assertEquals(LocalDate.of(2026, 10, 9), data.prevista)
        assertTrue(data.foiAntecipada)
    }

    @Test
    fun `dia em sabado mantido quando regra desligada`() {
        val data = calculadora.dataNoMes(renda(10), YearMonth.of(2026, 10), anteciparDiaNaoUtil = false)!!
        assertEquals(LocalDate.of(2026, 10, 10), data.prevista)
        assertFalse(data.foiAntecipada)
    }

    @Test
    fun `dia em feriado e antecipado`() {
        // 07/09/2026 segunda-feira, feriado → sexta 04/09
        val data = calculadora.dataNoMes(renda(7), YearMonth.of(2026, 9), anteciparDiaNaoUtil = true)
        assertEquals(LocalDate.of(2026, 9, 4), data?.prevista)
    }

    @Test
    fun `quinto dia util`() {
        val data = calculadora.dataNoMes(renda(5, ModoDiaRecebimento.DIA_UTIL), YearMonth.of(2026, 11), true)
        assertEquals(LocalDate.of(2026, 11, 9), data?.prevista)
    }

    @Test
    fun `fora da vigencia retorna null`() {
        val r = renda(10, inicio = LocalDate.of(2026, 10, 15), fim = LocalDate.of(2026, 12, 31))
        assertNull(calculadora.dataNoMes(r, YearMonth.of(2026, 10), true))
        assertNull(calculadora.dataNoMes(r, YearMonth.of(2027, 1), true))
        assertEquals(LocalDate.of(2026, 11, 10), calculadora.dataNoMes(r, YearMonth.of(2026, 11), true)?.prevista)
    }

    @Test
    fun `vigencia usa data nominal para nao perder recebimento antecipado`() {
        // Início em 10/10 (sábado); antecipação leva para 09/10, mas o recebimento conta.
        val r = renda(10, inicio = LocalDate.of(2026, 10, 10))
        assertEquals(LocalDate.of(2026, 10, 9), calculadora.dataNoMes(r, YearMonth.of(2026, 10), true)?.prevista)
    }

    @Test
    fun `proximo recebimento passa para o mes seguinte quando ja passou`() {
        // 20/10 já passou; 20/11/2026 é feriado (Consciência Negra) → antecipa para 19/11
        val proximo = calculadora.proximoRecebimento(renda(20), LocalDate.of(2026, 10, 22), true)
        assertEquals(LocalDate.of(2026, 11, 19), proximo?.prevista)
    }

    @Test
    fun `proximo recebimento atravessa a virada do ano`() {
        val proximo = calculadora.proximoRecebimento(
            renda(5, ModoDiaRecebimento.DIA_UTIL),
            LocalDate.of(2026, 12, 15),
            true,
        )
        assertEquals(LocalDate.of(2027, 1, 8), proximo?.prevista)
    }

    @Test
    fun `proximo recebimento no proprio dia conta`() {
        val proximo = calculadora.proximoRecebimento(renda(22), LocalDate.of(2026, 10, 22), true)
        assertEquals(LocalDate.of(2026, 10, 22), proximo?.prevista)
    }

    @Test
    fun `renda encerrada nao tem proximo recebimento`() {
        val r = renda(10, fim = LocalDate.of(2026, 9, 30))
        assertNull(calculadora.proximoRecebimento(r, LocalDate.of(2026, 10, 22), true))
    }
}
