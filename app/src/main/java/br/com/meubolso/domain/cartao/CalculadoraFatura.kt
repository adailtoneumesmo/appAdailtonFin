package br.com.meubolso.domain.cartao

import br.com.meubolso.domain.model.Cartao
import br.com.meubolso.domain.model.CompraCartao
import br.com.meubolso.domain.model.Fatura
import br.com.meubolso.domain.model.LancamentoFatura
import br.com.meubolso.domain.model.ModalidadeCompra
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class CalculadoraFatura @Inject constructor() {

    /** Data de fechamento da fatura do [mes]; dia inexistente vira o último dia do mês. */
    fun fechamento(cartao: Cartao, mes: YearMonth): LocalDate = mes.atDia(cartao.diaFechamento)

    /**
     * Vencimento da fatura do [mes]: o primeiro dia de vencimento depois do fechamento.
     * Ex.: fecha dia 10 e vence dia 17 → mesmo mês; fecha dia 28 e vence dia 5 → mês seguinte.
     */
    fun vencimento(cartao: Cartao, mes: YearMonth): LocalDate {
        val noMes = mes.atDia(cartao.diaVencimento)
        return if (noMes.isAfter(fechamento(cartao, mes))) noMes else mes.plusMonths(1).atDia(cartao.diaVencimento)
    }

    /** Compra antes do fechamento entra na fatura do mês; no dia do fechamento ou depois, na seguinte. */
    fun faturaDaCompra(cartao: Cartao, data: LocalDate): YearMonth {
        val mes = YearMonth.from(data)
        return if (data.isBefore(fechamento(cartao, mes))) mes else mes.plusMonths(1)
    }

    /** Divide [totalCentavos] em [parcelas] iguais; a diferença de centavos fica na última. */
    fun dividirEmParcelas(totalCentavos: Long, parcelas: Int): List<Long> {
        require(parcelas > 0) { "Quantidade de parcelas deve ser positiva" }
        val base = totalCentavos / parcelas
        return List(parcelas) { indice -> if (indice == parcelas - 1) totalCentavos - base * (parcelas - 1) else base }
    }

    /** Lançamento da compra na fatura do [mes], ou `null` se a compra não é cobrada nela. */
    fun lancamentoNaFatura(compra: CompraCartao, mes: YearMonth): LancamentoFatura? {
        val indice = ChronoUnit.MONTHS.between(compra.mesPrimeiraFatura, mes)
        if (indice < 0) return null
        return when (compra.modalidade) {
            ModalidadeCompra.A_VISTA ->
                if (indice == 0L) LancamentoFatura(compra, compra.valorCentavos, parcela = null) else null
            ModalidadeCompra.PARCELADO -> {
                val total = requireNotNull(compra.totalParcelas) { "Compra parcelada sem total de parcelas" }
                if (indice >= total) return null
                val valor = dividirEmParcelas(compra.valorCentavos, total)[indice.toInt()]
                LancamentoFatura(compra, valor, parcela = indice.toInt() + 1)
            }
            ModalidadeCompra.FIXO -> {
                val ultima = compra.mesUltimaFatura
                if (ultima != null && mes > ultima) null else LancamentoFatura(compra, compra.valorCentavos, parcela = null)
            }
        }
    }

    /** Fatura do [cartao] no [mes] com os lançamentos ordenados por data da compra. */
    fun fatura(cartao: Cartao, compras: List<CompraCartao>, mes: YearMonth): Fatura = Fatura(
        cartao = cartao,
        mes = mes,
        fechamento = fechamento(cartao, mes),
        vencimento = vencimento(cartao, mes),
        lancamentos = compras
            .filter { it.cartaoId == cartao.id }
            .mapNotNull { lancamentoNaFatura(it, mes) }
            .sortedWith(compareBy({ it.compra.dataCompra }, { it.compra.descricao.lowercase() })),
    )

    private fun YearMonth.atDia(dia: Int): LocalDate = atDay(minOf(dia, lengthOfMonth()))
}
