package br.com.meubolso.domain.model

import java.time.LocalDate
import java.time.YearMonth

data class Cartao(
    val id: Long = 0,
    val nome: String,
    /** 1–31; em meses mais curtos fecha no último dia. */
    val diaFechamento: Int,
    /** 1–31; em meses mais curtos vence no último dia. */
    val diaVencimento: Int,
    /** Com qual renda a fatura é paga. */
    val fonte: Origem,
)

enum class ModalidadeCompra { A_VISTA, PARCELADO, FIXO }

/**
 * Uma compra lançada no cartão. As parcelas e as cobranças mensais de uma compra fixa
 * não são gravadas uma a uma: são derivadas desta linha em [br.com.meubolso.domain.cartao.CalculadoraFatura],
 * então editar ou excluir a compra atualiza todas as faturas de uma vez.
 */
data class CompraCartao(
    val id: Long = 0,
    val cartaoId: Long,
    val descricao: String,
    val categoriaId: Long,
    /** Valor total da compra; no parcelado é dividido entre as parcelas. Na fixa, é o valor mensal. */
    val valorCentavos: Long,
    val dataCompra: LocalDate,
    val modalidade: ModalidadeCompra,
    /** Obrigatório quando [modalidade] é [ModalidadeCompra.PARCELADO]. */
    val totalParcelas: Int? = null,
    /**
     * Fatura em que a compra (ou a 1ª parcela) entrou, calculada ao salvar. Fica gravada para
     * que mudar o dia de fechamento do cartão não mova compras já lançadas.
     */
    val mesPrimeiraFatura: YearMonth,
    /** Só para compra fixa: última fatura cobrada depois de encerrada. `null` = recorrência ativa. */
    val mesUltimaFatura: YearMonth? = null,
)

/** A compra materializada em uma fatura específica. */
data class LancamentoFatura(
    val compra: CompraCartao,
    val valorCentavos: Long,
    /** Número da parcela (1-based) quando parcelada. */
    val parcela: Int?,
) {
    /** Parcelas que ainda virão depois desta. */
    val parcelasRestantes: Int? get() = parcela?.let { (compra.totalParcelas ?: it) - it }
}

data class Fatura(
    val cartao: Cartao,
    val mes: YearMonth,
    val fechamento: LocalDate,
    val vencimento: LocalDate,
    val lancamentos: List<LancamentoFatura>,
) {
    val totalCentavos: Long get() = lancamentos.sumOf { it.valorCentavos }

    /** Depois do fechamento a fatura não recebe mais compras. */
    fun fechada(hoje: LocalDate): Boolean = !hoje.isBefore(fechamento)
}
