package br.com.meubolso.domain.calendario

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CalendarioDiasUteisTest {

    private val calendario = CalendarioDiasUteis()

    @Test
    fun `pascoa calculada corretamente`() {
        assertEquals(LocalDate.of(2024, 3, 31), FeriadosNacionais.pascoa(2024))
        assertEquals(LocalDate.of(2025, 4, 20), FeriadosNacionais.pascoa(2025))
        assertEquals(LocalDate.of(2026, 4, 5), FeriadosNacionais.pascoa(2026))
        assertEquals(LocalDate.of(2027, 3, 28), FeriadosNacionais.pascoa(2027))
    }

    @Test
    fun `feriados nacionais incluem sexta-feira santa e consciencia negra`() {
        val feriados = FeriadosNacionais.doAno(2026)
        assertEquals(10, feriados.size)
        assertTrue(LocalDate.of(2026, 4, 3) in feriados)
        assertTrue(LocalDate.of(2026, 11, 20) in feriados)
    }

    @Test
    fun `fim de semana e feriado nao sao dias uteis`() {
        assertFalse(calendario.ehDiaUtil(LocalDate.of(2026, 10, 10))) // sábado
        assertFalse(calendario.ehDiaUtil(LocalDate.of(2026, 11, 1)))  // domingo
        assertFalse(calendario.ehDiaUtil(LocalDate.of(2026, 9, 7)))   // feriado em segunda
        assertTrue(calendario.ehDiaUtil(LocalDate.of(2026, 10, 22)))  // quinta comum
    }

    @Test
    fun `dia util anterior pula fim de semana e feriado`() {
        // 08/09/2026 é terça; 07/09 (segunda) é feriado → recua até sexta 04/09
        assertEquals(LocalDate.of(2026, 9, 4), calendario.diaUtilAnteriorOuMesmo(LocalDate.of(2026, 9, 7)))
        assertEquals(LocalDate.of(2026, 10, 9), calendario.diaUtilAnteriorOuMesmo(LocalDate.of(2026, 10, 11)))
        assertEquals(LocalDate.of(2026, 10, 22), calendario.diaUtilAnteriorOuMesmo(LocalDate.of(2026, 10, 22)))
    }

    @Test
    fun `quinto dia util de outubro de 2026`() {
        // 01/10 é quinta: 1, 2, 5, 6, 7
        assertEquals(LocalDate.of(2026, 10, 7), calendario.enesimoDiaUtil(YearMonth.of(2026, 10), 5))
    }

    @Test
    fun `quinto dia util desconta feriado de finados`() {
        // 01/11 domingo, 02/11 feriado: 3, 4, 5, 6, 9
        assertEquals(LocalDate.of(2026, 11, 9), calendario.enesimoDiaUtil(YearMonth.of(2026, 11), 5))
    }

    @Test
    fun `quinto dia util na virada do ano desconta primeiro de janeiro`() {
        // 01/01/2027 sexta e feriado: 4, 5, 6, 7, 8
        assertEquals(LocalDate.of(2027, 1, 8), calendario.enesimoDiaUtil(YearMonth.of(2027, 1), 5))
    }

    @Test
    fun `novembro de 2026 tem 19 dias uteis`() {
        // 21 dias de seg a sex, menos Finados (02/11, segunda) e Consciência Negra (20/11, sexta);
        // Proclamação da República (15/11) cai em domingo.
        assertEquals(19, calendario.quantidadeNoMes(YearMonth.of(2026, 11)))
    }

    @Test
    fun `abril de 2026 desconta sexta-feira santa e tiradentes`() {
        // 22 dias de seg a sex, menos 03/04 (Sexta-feira Santa) e 21/04 (Tiradentes, terça)
        assertEquals(20, calendario.quantidadeNoMes(YearMonth.of(2026, 4)))
    }

    @Test
    fun `mes sem feriados conta apenas seg a sex`() {
        // Agosto/2026: começa num sábado, 21 dias de seg a sex, nenhum feriado nacional
        assertEquals(21, calendario.quantidadeNoMes(YearMonth.of(2026, 8)))
    }

    @Test
    fun `fevereiro de ano bissexto inclui o dia 29`() {
        // Fevereiro/2028: 29 dias começando numa terça → 21 dias de seg a sex (29/02 é terça)
        assertEquals(21, calendario.quantidadeNoMes(YearMonth.of(2028, 2)))
        assertTrue(calendario.ehDiaUtil(LocalDate.of(2028, 2, 29)))
    }

    @Test
    fun `fontes extras de feriados entram na contagem`() {
        val carnaval2027 = FonteFeriados { ano -> setOf(FeriadosNacionais.pascoa(ano).minusDays(47)) }
        val comCarnaval = CalendarioDiasUteis(listOf(FeriadosNacionais, carnaval2027))
        // Carnaval 2027: terça 09/02
        assertEquals(calendario.quantidadeNoMes(YearMonth.of(2027, 2)) - 1, comCarnaval.quantidadeNoMes(YearMonth.of(2027, 2)))
    }

    @Test
    fun `n maior que dias uteis do mes retorna ultimo dia util`() {
        // 28/02/2026 é sábado → último dia útil é 27/02
        assertEquals(LocalDate.of(2026, 2, 27), calendario.enesimoDiaUtil(YearMonth.of(2026, 2), 23))
    }
}
