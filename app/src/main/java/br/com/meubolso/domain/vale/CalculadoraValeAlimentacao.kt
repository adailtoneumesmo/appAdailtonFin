package br.com.meubolso.domain.vale

import br.com.meubolso.domain.calendario.CalendarioDiasUteis
import br.com.meubolso.domain.despesa.CalculadoraOcorrencias
import br.com.meubolso.domain.model.DespesaFixa
import br.com.meubolso.domain.model.FormaCalculoVale
import br.com.meubolso.domain.model.OcorrenciaDespesa
import br.com.meubolso.domain.model.PagamentoDespesa
import br.com.meubolso.domain.model.Renda
import br.com.meubolso.domain.model.TipoRenda
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * Valor do VR/VA em um mês de referência.
 * No cálculo por dia, [diasUteis] é a quantidade de dias úteis de [mesDiasUteis] (o mês seguinte).
 */
data class ValorVale(
    val totalCentavos: Long,
    val forma: FormaCalculoVale,
    val valorDiarioCentavos: Long? = null,
    val diasUteis: Int? = null,
    val mesDiasUteis: YearMonth? = null,
)

data class CreditoVale(val renda: Renda, val data: LocalDate, val valor: ValorVale)

/** Carteira do VR/VA no mês: créditos − despesas pagas com o vale. Fica fora do saldo comum. */
data class SaldoVale(
    val creditos: List<CreditoVale> = emptyList(),
    val despesas: List<OcorrenciaDespesa> = emptyList(),
) {
    val creditoCentavos: Long get() = creditos.sumOf { it.valor.totalCentavos }

    val gastoCentavos: Long get() = despesas.sumOf { it.despesa.valorCentavos }

    val saldoCentavos: Long get() = creditoCentavos - gastoCentavos

    val vazio: Boolean get() = creditos.isEmpty() && despesas.isEmpty()
}

class CalculadoraValeAlimentacao @Inject constructor(
    private val calendario: CalendarioDiasUteis,
    private val calculadoraOcorrencias: CalculadoraOcorrencias,
) {

    /**
     * Valor fixo: o próprio valor. Por dia: valor diário × dias úteis do mês seguinte a [mes]
     * (ex.: referência outubro/2026, R$ 20,00/dia × 19 dias úteis de novembro = R$ 380,00).
     */
    fun valorNoMes(forma: FormaCalculoVale, valorCentavos: Long, mes: YearMonth): ValorVale = when (forma) {
        FormaCalculoVale.VALOR_FIXO -> ValorVale(totalCentavos = valorCentavos, forma = forma)
        FormaCalculoVale.VALOR_POR_DIA -> {
            val seguinte = mes.plusMonths(1)
            val dias = calendario.quantidadeNoMes(seguinte)
            ValorVale(
                totalCentavos = Math.multiplyExact(valorCentavos, dias.toLong()),
                forma = forma,
                valorDiarioCentavos = valorCentavos,
                diasUteis = dias,
                mesDiasUteis = seguinte,
            )
        }
    }

    fun valorNoMes(renda: Renda, mes: YearMonth): ValorVale =
        valorNoMes(renda.formaCalculoVale ?: FormaCalculoVale.VALOR_FIXO, renda.valorCentavos, mes)

    /** Dia de disponibilização no [mes] (limitado ao último dia), ou `null` fora da vigência. */
    fun dataNoMes(renda: Renda, mes: YearMonth): LocalDate? =
        mes.atDay(minOf(renda.dia, mes.lengthOfMonth())).takeIf { renda.vigenteEm(it) }

    /**
     * Créditos de VR/VA ativos no [mes] contra todas as despesas do mês pagas com o vale
     * (pagas ou ainda pendentes, como no restante do painel).
     * O crédito usado no [mes] é o disponibilizado no mês anterior, calculado com os dias úteis do
     * próprio [mes] (ex.: novembro/2026 = 19 dias úteis de novembro).
     */
    fun saldoNoMes(
        rendas: List<Renda>,
        despesas: List<DespesaFixa>,
        pagamentos: List<PagamentoDespesa>,
        mes: YearMonth,
    ): SaldoVale = SaldoVale(
        creditos = rendas
            .filter { it.ativo && it.tipo == TipoRenda.VALE_ALIMENTACAO }
            .mapNotNull { renda -> dataNoMes(renda, mes)?.let { CreditoVale(renda, it, valorNoMes(renda, mes.minusMonths(1))) } }
            .sortedBy { it.data },
        despesas = calculadoraOcorrencias.ocorrenciasNoMes(despesas.filter { it.pagaComValeAlimentacao }, pagamentos, mes),
    )
}
