package br.com.meubolso.domain.model

import java.time.LocalDate
import java.time.YearMonth

enum class FormaPagamento { PIX, BOLETO, DEBITO_AUTOMATICO, CARTAO_CREDITO, VALE_ALIMENTACAO }

/** Com qual renda a despesa será paga. */
enum class VinculoPeriodo {
    /** Decide pelo período em que a data de vencimento cai. */
    AUTOMATICO,
    SALARIO,
    VALE,
}

/** [UNICA]: uma cobrança só, no [DespesaFixa.mesInicio] (ex.: compra de mercado). */
enum class Recorrencia { MENSAL, PARCELADA, UNICA }

data class Categoria(
    val id: Long = 0,
    val nome: String,
    /** Cor ARGB usada em listas e gráficos. */
    val cor: Int,
    /** Categorias padrão não podem ser editadas nem excluídas. */
    val padrao: Boolean = false,
)

data class DespesaFixa(
    val id: Long = 0,
    val descricao: String,
    val categoriaId: Long,
    val valorCentavos: Long,
    /** 1–31; em meses mais curtos vence no último dia. */
    val diaVencimento: Int,
    val formaPagamento: FormaPagamento,
    val vinculo: VinculoPeriodo = VinculoPeriodo.AUTOMATICO,
    val recorrencia: Recorrencia = Recorrencia.MENSAL,
    /** Obrigatório quando [recorrencia] é [Recorrencia.PARCELADA]. */
    val totalParcelas: Int? = null,
    /** Mês da primeira cobrança (ou da 1ª parcela). */
    val mesInicio: YearMonth,
    val ativa: Boolean = true,
) {
    /** Paga com o VR/VA: debita da carteira do vale, não do salário. */
    val pagaComValeAlimentacao: Boolean get() = formaPagamento == FormaPagamento.VALE_ALIMENTACAO
}

/** Uma ocorrência mensal de despesa marcada como paga. */
data class PagamentoDespesa(
    val despesaId: Long,
    val competencia: YearMonth,
    val dataPagamento: LocalDate,
)

/** A despesa fixa materializada em um mês específico. */
data class OcorrenciaDespesa(
    val despesa: DespesaFixa,
    val competencia: YearMonth,
    val vencimento: LocalDate,
    /** Número da parcela (1-based) quando parcelada. */
    val parcela: Int?,
    val dataPagamento: LocalDate?,
) {
    val paga: Boolean get() = dataPagamento != null

    fun vencida(hoje: LocalDate): Boolean = !paga && vencimento.isBefore(hoje)
}
