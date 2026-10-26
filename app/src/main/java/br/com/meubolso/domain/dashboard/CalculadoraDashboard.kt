package br.com.meubolso.domain.dashboard

import br.com.meubolso.domain.cartao.CalculadoraFatura
import br.com.meubolso.domain.despesa.CalculadoraOcorrencias
import br.com.meubolso.domain.model.Cartao
import br.com.meubolso.domain.model.CompraCartao
import br.com.meubolso.domain.model.DespesaFixa
import br.com.meubolso.domain.model.Fatura
import br.com.meubolso.domain.model.OcorrenciaDespesa
import br.com.meubolso.domain.model.Origem
import br.com.meubolso.domain.model.PagamentoDespesa
import br.com.meubolso.domain.model.Renda
import br.com.meubolso.domain.model.TipoRenda
import br.com.meubolso.domain.model.VinculoPeriodo
import br.com.meubolso.domain.model.origem
import br.com.meubolso.domain.renda.CalculadoraRecebimento
import br.com.meubolso.domain.vale.CalculadoraValeAlimentacao
import br.com.meubolso.domain.vale.SaldoVale
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

data class RendaNoMes(val renda: Renda, val data: LocalDate)

/** Os lançamentos que compõem o [Balanco] de uma origem no mês. */
data class DetalheOrigem(
    val rendas: List<RendaNoMes> = emptyList(),
    val despesas: List<OcorrenciaDespesa> = emptyList(),
    /** Faturas de cartão que vencem no mês; entram como uma despesa só, pelo total. */
    val faturas: List<Fatura> = emptyList(),
) {
    val balanco: Balanco
        get() = Balanco(
            rendaCentavos = rendas.sumOf { it.renda.valorCentavos },
            despesaCentavos = despesas.sumOf { it.despesa.valorCentavos } + faturas.sumOf { it.totalCentavos },
        )

    val vazio: Boolean get() = rendas.isEmpty() && despesas.isEmpty() && faturas.isEmpty()
}

data class ResumoDashboard(
    val detalheSalario: DetalheOrigem = DetalheOrigem(),
    val detalheVale: DetalheOrigem = DetalheOrigem(),
    /** Parte de [salario] que veio de renda extra. */
    val rendaExtraCentavos: Long = 0,
    /** Carteira do VR/VA: não entra em [geral]. */
    val valeAlimentacao: SaldoVale = SaldoVale(),
) {
    val salario: Balanco get() = detalheSalario.balanco

    val vale: Balanco get() = detalheVale.balanco

    val geral: Balanco get() = salario + vale

    val possuiLancamentos: Boolean get() = !detalheSalario.vazio || !detalheVale.vazio || !valeAlimentacao.vazio
}

class CalculadoraDashboard @Inject constructor(
    private val calculadoraRecebimento: CalculadoraRecebimento,
    private val calculadoraOcorrencias: CalculadoraOcorrencias,
    private val calculadoraFatura: CalculadoraFatura,
    private val calculadoraVale: CalculadoraValeAlimentacao,
) {

    /**
     * Rendas ativas recebidas no [mes] contra todas as despesas do [mes] (pagas ou não) e as
     * faturas de cartão que vencem nele, separadas por [Origem]. O VR/VA (rendas e despesas
     * pagas com ele) fica de fora das origens e é resumido à parte em [ResumoDashboard.valeAlimentacao].
     */
    fun resumir(
        rendas: List<Renda>,
        despesas: List<DespesaFixa>,
        pagamentos: List<PagamentoDespesa>,
        mes: YearMonth,
        anteciparDiaNaoUtil: Boolean,
        cartoes: List<Cartao> = emptyList(),
        compras: List<CompraCartao> = emptyList(),
    ): ResumoDashboard {
        val ativas = rendas.filter { it.ativo && it.tipo.origem != null }
        val recebidasNoMes = ativas
            .mapNotNull { renda ->
                calculadoraRecebimento.dataNoMes(renda, mes, anteciparDiaNaoUtil)?.let { RendaNoMes(renda, it.prevista) }
            }
            .sortedBy { it.data }
        val ocorrencias = calculadoraOcorrencias.ocorrenciasNoMes(despesas.filterNot { it.pagaComValeAlimentacao }, pagamentos, mes)
        val inicios = iniciosDePeriodo(ativas, mes, anteciparDiaNaoUtil)
        val faturas = faturasQueVencemNoMes(cartoes, compras, mes)

        val rendasPorOrigem = recebidasNoMes.groupBy { it.renda.tipo.origem }
        val despesasPorOrigem = ocorrencias.groupBy { origemDaDespesa(it.despesa.vinculo, it.vencimento, inicios) }
        val faturasPorOrigem = faturas.groupBy { it.cartao.fonte }

        fun detalhe(origem: Origem) = DetalheOrigem(
            rendas = rendasPorOrigem[origem].orEmpty(),
            despesas = despesasPorOrigem[origem].orEmpty(),
            faturas = faturasPorOrigem[origem].orEmpty(),
        )

        return ResumoDashboard(
            detalheSalario = detalhe(Origem.SALARIO),
            detalheVale = detalhe(Origem.VALE),
            rendaExtraCentavos = recebidasNoMes
                .filter { it.renda.tipo == TipoRenda.RENDA_EXTRA }
                .sumOf { it.renda.valorCentavos },
            valeAlimentacao = calculadoraVale.saldoNoMes(rendas, despesas, pagamentos, mes),
        )
    }

    /**
     * A fatura do mês de referência M vence em M ou em M+1, então basta olhar as faturas do
     * mês anterior e do atual. Faturas sem lançamentos não aparecem.
     */
    private fun faturasQueVencemNoMes(cartoes: List<Cartao>, compras: List<CompraCartao>, mes: YearMonth): List<Fatura> =
        cartoes
            .flatMap { cartao -> listOf(mes.minusMonths(1), mes).map { calculadoraFatura.fatura(cartao, compras, it) } }
            .filter { YearMonth.from(it.vencimento) == mes && it.lancamentos.isNotEmpty() }
            .sortedWith(compareBy({ it.vencimento }, { it.cartao.nome.lowercase() }))

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
                    ?.let { InicioPeriodo(it.prevista, checkNotNull(renda.tipo.origem)) }
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
