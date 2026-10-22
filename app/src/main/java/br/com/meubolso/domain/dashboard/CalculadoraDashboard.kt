package br.com.meubolso.domain.dashboard

import br.com.meubolso.domain.despesa.CalculadoraOcorrencias
import br.com.meubolso.domain.model.DespesaFixa
import br.com.meubolso.domain.model.Origem
import br.com.meubolso.domain.model.PagamentoDespesa
import br.com.meubolso.domain.model.Renda
import br.com.meubolso.domain.model.TipoRenda
import br.com.meubolso.domain.model.VinculoPeriodo
import br.com.meubolso.domain.model.origem
import br.com.meubolso.domain.renda.CalculadoraRecebimento
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

enum class SituacaoSaldo { POSITIVO, NEGATIVO, ZERADO }

/** Quanto da renda já foi consumido: até 70% normal, até 100% alerta, acima disso excedido. */
enum class FaixaConsumo { NORMAL, ALERTA, EXCEDIDO }

/** Renda x despesa de uma origem (ou do mês inteiro). */
data class Balanco(val rendaCentavos: Long = 0, val despesaCentavos: Long = 0) {

    val saldoCentavos: Long get() = rendaCentavos - despesaCentavos

    val situacao: SituacaoSaldo
        get() = when {
            saldoCentavos > 0 -> SituacaoSaldo.POSITIVO
            saldoCentavos < 0 -> SituacaoSaldo.NEGATIVO
            else -> SituacaoSaldo.ZERADO
        }

    /** Despesa / renda (1.0 = 100%). Com despesa e sem renda, é infinito (tudo excedido). */
    val fracaoConsumida: Double
        get() = when {
            despesaCentavos == 0L -> 0.0
            rendaCentavos <= 0L -> Double.POSITIVE_INFINITY
            else -> despesaCentavos.toDouble() / rendaCentavos
        }

    val faixaConsumo: FaixaConsumo
        get() = when {
            fracaoConsumida <= LIMITE_ALERTA -> FaixaConsumo.NORMAL
            fracaoConsumida <= 1.0 -> FaixaConsumo.ALERTA
            else -> FaixaConsumo.EXCEDIDO
        }

    operator fun plus(outro: Balanco) =
        Balanco(rendaCentavos + outro.rendaCentavos, despesaCentavos + outro.despesaCentavos)

    companion object {
        const val LIMITE_ALERTA = 0.7
    }
}

data class ResumoDashboard(
    val salario: Balanco = Balanco(),
    val vale: Balanco = Balanco(),
    /** Parte de [salario] que veio de renda extra. */
    val rendaExtraCentavos: Long = 0,
    val possuiLancamentos: Boolean = false,
) {
    val geral: Balanco get() = salario + vale
}

class CalculadoraDashboard @Inject constructor(
    private val calculadoraRecebimento: CalculadoraRecebimento,
    private val calculadoraOcorrencias: CalculadoraOcorrencias,
) {

    /**
     * Rendas ativas recebidas no [mes] contra todas as despesas do [mes] (pagas ou não),
     * separadas por [Origem].
     */
    fun resumir(
        rendas: List<Renda>,
        despesas: List<DespesaFixa>,
        pagamentos: List<PagamentoDespesa>,
        mes: YearMonth,
        anteciparDiaNaoUtil: Boolean,
    ): ResumoDashboard {
        val ativas = rendas.filter { it.ativo }
        val recebidasNoMes = ativas.filter { calculadoraRecebimento.dataNoMes(it, mes, anteciparDiaNaoUtil) != null }
        val ocorrencias = calculadoraOcorrencias.ocorrenciasNoMes(despesas, pagamentos, mes)
        val inicios = iniciosDePeriodo(ativas, mes, anteciparDiaNaoUtil)

        val rendaPorOrigem = recebidasNoMes
            .groupBy { it.tipo.origem }
            .mapValues { (_, lista) -> lista.sumOf { it.valorCentavos } }
        val despesaPorOrigem = ocorrencias
            .groupBy { origemDaDespesa(it.despesa.vinculo, it.vencimento, inicios) }
            .mapValues { (_, lista) -> lista.sumOf { it.despesa.valorCentavos } }

        fun balanco(origem: Origem) = Balanco(rendaPorOrigem[origem] ?: 0, despesaPorOrigem[origem] ?: 0)

        return ResumoDashboard(
            salario = balanco(Origem.SALARIO),
            vale = balanco(Origem.VALE),
            rendaExtraCentavos = recebidasNoMes.filter { it.tipo == TipoRenda.RENDA_EXTRA }.sumOf { it.valorCentavos },
            possuiLancamentos = recebidasNoMes.isNotEmpty() || ocorrencias.isNotEmpty(),
        )
    }

    /**
     * Datas previstas de salário e vale no mês anterior e no atual: cada uma abre um período
     * que vai até o próximo recebimento. O mês anterior cobre vencimentos do começo do mês.
     */
    private fun iniciosDePeriodo(
        rendas: List<Renda>,
        mes: YearMonth,
        anteciparDiaNaoUtil: Boolean,
    ): List<InicioPeriodo> = rendas
        .filter { it.tipo != TipoRenda.RENDA_EXTRA }
        .flatMap { renda ->
            listOf(mes.minusMonths(1), mes).mapNotNull { m ->
                calculadoraRecebimento.dataNoMes(renda, m, anteciparDiaNaoUtil)
                    ?.let { InicioPeriodo(it.prevista, renda.tipo.origem) }
            }
        }

    /**
     * Vínculo explícito vence; no AUTOMATICO a despesa sai do último recebimento até o
     * vencimento (no mesmo dia, salário tem preferência). Sem recebimento anterior, salário.
     */
    private fun origemDaDespesa(vinculo: VinculoPeriodo, vencimento: LocalDate, inicios: List<InicioPeriodo>): Origem =
        when (vinculo) {
            VinculoPeriodo.SALARIO -> Origem.SALARIO
            VinculoPeriodo.VALE -> Origem.VALE
            VinculoPeriodo.AUTOMATICO -> inicios
                .filter { !it.data.isAfter(vencimento) }
                .maxWithOrNull(compareBy({ it.data }, { it.origem == Origem.SALARIO }))
                ?.origem
                ?: Origem.SALARIO
        }

    private data class InicioPeriodo(val data: LocalDate, val origem: Origem)
}
