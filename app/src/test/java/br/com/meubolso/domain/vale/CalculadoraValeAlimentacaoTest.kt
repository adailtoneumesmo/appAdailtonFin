package br.com.meubolso.domain.vale

import br.com.meubolso.domain.calendario.CalendarioDiasUteis
import br.com.meubolso.domain.despesa.CalculadoraOcorrencias
import br.com.meubolso.domain.model.DespesaFixa
import br.com.meubolso.domain.model.FormaCalculoVale
import br.com.meubolso.domain.model.FormaPagamento
import br.com.meubolso.domain.model.ModoDiaRecebimento
import br.com.meubolso.domain.model.PagamentoDespesa
import br.com.meubolso.domain.model.Recorrencia
import br.com.meubolso.domain.model.Renda
import br.com.meubolso.domain.model.TipoRenda
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CalculadoraValeAlimentacaoTest {

    private val calculadora = CalculadoraValeAlimentacao(CalendarioDiasUteis(), CalculadoraOcorrencias())

    private val outubro = YearMonth.of(2026, 10)

    private fun vale(
        valor: Long,
        forma: FormaCalculoVale,
        dia: Int = 25,
        id: Long = 1,
        ativo: Boolean = true,
        inicio: LocalDate = LocalDate.of(2026, 1, 1),
    ) = Renda(
        id = id,
        descricao = "VR",
        tipo = TipoRenda.VALE_ALIMENTACAO,
        valorCentavos = valor,
        modoDia = ModoDiaRecebimento.DIA_FIXO,
        dia = dia,
        ativo = ativo,
        dataInicio = inicio,
        formaCalculoVale = forma,
    )

    private fun despesa(
        id: Long,
        valor: Long,
        forma: FormaPagamento = FormaPagamento.VALE_ALIMENTACAO,
        recorrencia: Recorrencia = Recorrencia.MENSAL,
        inicio: YearMonth = YearMonth.of(2026, 1),
    ) = DespesaFixa(
        id = id,
        descricao = "Despesa $id",
        categoriaId = 1,
        valorCentavos = valor,
        diaVencimento = 10,
        formaPagamento = forma,
        recorrencia = recorrencia,
        mesInicio = inicio,
    )

    @Test
    fun `por dia em outubro de 2026 usa os 19 dias uteis de novembro`() {
        val valor = calculadora.valorNoMes(FormaCalculoVale.VALOR_POR_DIA, 2_000, outubro)
        assertEquals(38_000L, valor.totalCentavos)
        assertEquals(19, valor.diasUteis)
        assertEquals(YearMonth.of(2026, 11), valor.mesDiasUteis)
        assertEquals(2_000L, valor.valorDiarioCentavos)
    }

    @Test
    fun `por dia em dezembro usa os dias uteis de janeiro do ano seguinte`() {
        // Janeiro/2027: 21 dias de seg a sex, menos 01/01 (sexta) = 20
        val valor = calculadora.valorNoMes(FormaCalculoVale.VALOR_POR_DIA, 1_000, YearMonth.of(2026, 12))
        assertEquals(20, valor.diasUteis)
        assertEquals(20_000L, valor.totalCentavos)
    }

    @Test
    fun `valor fixo nao depende de dias uteis`() {
        val valor = calculadora.valorNoMes(FormaCalculoVale.VALOR_FIXO, 80_000, outubro)
        assertEquals(80_000L, valor.totalCentavos)
        assertNull(valor.diasUteis)
    }

    @Test
    fun `dia 31 de disponibilizacao vira ultimo dia em meses curtos`() {
        val renda = vale(80_000, FormaCalculoVale.VALOR_FIXO, dia = 31)
        assertEquals(LocalDate.of(2026, 11, 30), calculadora.dataNoMes(renda, YearMonth.of(2026, 11)))
        assertEquals(LocalDate.of(2026, 2, 28), calculadora.dataNoMes(renda, YearMonth.of(2026, 2)))
    }

    @Test
    fun `saldo desconta so despesas pagas com o vale`() {
        val saldo = calculadora.saldoNoMes(
            rendas = listOf(vale(2_000, FormaCalculoVale.VALOR_POR_DIA)),
            despesas = listOf(
                despesa(1, 15_000),
                despesa(2, 50_000, forma = FormaPagamento.PIX),
                despesa(3, 5_000, recorrencia = Recorrencia.UNICA, inicio = outubro),
            ),
            pagamentos = emptyList(),
            mes = outubro,
        )
        // Outubro/2026: 21 dias úteis (12/10 é feriado)
        assertEquals(42_000L, saldo.creditoCentavos)
        assertEquals(20_000L, saldo.gastoCentavos)
        assertEquals(22_000L, saldo.saldoCentavos)
    }

    @Test
    fun `saldo de novembro de 2026 usa os 19 dias uteis de novembro`() {
        val saldo = calculadora.saldoNoMes(
            rendas = listOf(vale(2_000, FormaCalculoVale.VALOR_POR_DIA)),
            despesas = emptyList(),
            pagamentos = emptyList(),
            mes = YearMonth.of(2026, 11),
        )
        assertEquals(38_000L, saldo.creditoCentavos)
        assertEquals(19, saldo.creditos.single().valor.diasUteis)
        assertEquals(YearMonth.of(2026, 11), saldo.creditos.single().valor.mesDiasUteis)
    }

    @Test
    fun `despesa pendente tambem entra no gasto e saldo pode ficar negativo`() {
        val pagamento = listOf(PagamentoDespesa(1, outubro, LocalDate.of(2026, 10, 10)))
        val saldo = calculadora.saldoNoMes(
            rendas = listOf(vale(10_000, FormaCalculoVale.VALOR_FIXO)),
            despesas = listOf(despesa(1, 8_000), despesa(2, 5_000)),
            pagamentos = pagamento,
            mes = outubro,
        )
        assertEquals(13_000L, saldo.gastoCentavos)
        assertEquals(-3_000L, saldo.saldoCentavos)
    }

    @Test
    fun `vale inativo ou fora da vigencia nao gera credito`() {
        val saldo = calculadora.saldoNoMes(
            rendas = listOf(
                vale(10_000, FormaCalculoVale.VALOR_FIXO, id = 1, ativo = false),
                vale(10_000, FormaCalculoVale.VALOR_FIXO, id = 2, inicio = LocalDate.of(2026, 11, 1)),
            ),
            despesas = emptyList(),
            pagamentos = emptyList(),
            mes = outubro,
        )
        assertTrue(saldo.vazio)
        assertEquals(0L, saldo.saldoCentavos)
    }
}
