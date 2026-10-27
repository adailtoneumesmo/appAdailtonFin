package br.com.meubolso.domain.objetivo

import br.com.meubolso.domain.model.Objetivo
import br.com.meubolso.domain.model.PosicaoInvestimentos
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

data class ProgressoObjetivo(
    val objetivo: Objetivo,
    val realizadoCentavos: Long,
    val faltaCentavos: Long,
    /** Quanto guardar por mês até a data-alvo; nulo sem data-alvo, concluído ou com prazo vencido. */
    val porMesCentavos: Long?,
    val prazoVencido: Boolean,
) {
    val concluido: Boolean get() = faltaCentavos == 0L
    val fracao: Double get() = fracao(realizadoCentavos, objetivo.valorAlvoCentavos)
}

data class ResumoObjetivos(
    val posicao: PosicaoInvestimentos? = null,
    val itens: List<ProgressoObjetivo> = emptyList(),
) {
    val patrimonioCentavos: Long get() = posicao?.valorCentavos ?: 0L
    val alvoCentavos: Long get() = itens.sumOf { it.objetivo.valorAlvoCentavos }
    val realizadoCentavos: Long get() = itens.sumOf { it.realizadoCentavos }
    val faltaCentavos: Long get() = itens.sumOf { it.faltaCentavos }

    /** Patrimônio além da soma de todos os objetivos. */
    val excedenteCentavos: Long get() = (patrimonioCentavos - alvoCentavos).coerceAtLeast(0L)
    val fracao: Double get() = fracao(realizadoCentavos, alvoCentavos)
}

/**
 * Todos os objetivos olham o mesmo patrimônio investido. Para a soma não contar o mesmo
 * dinheiro duas vezes, ele é distribuído em ordem de prioridade: primeiro o objetivo com a
 * data-alvo mais próxima, os sem data por último (empate: o cadastrado antes).
 */
object CalculadoraObjetivos {

    fun resumir(objetivos: List<Objetivo>, posicao: PosicaoInvestimentos?, hoje: LocalDate): ResumoObjetivos {
        var disponivel = posicao?.valorCentavos ?: 0L
        val itens = ordenarPorPrioridade(objetivos).map { objetivo ->
            val realizado = disponivel.coerceIn(0L, objetivo.valorAlvoCentavos)
            disponivel -= realizado
            progresso(objetivo, realizado, hoje)
        }
        return ResumoObjetivos(posicao, itens)
    }

    fun ordenarPorPrioridade(objetivos: List<Objetivo>): List<Objetivo> =
        objetivos.sortedWith(compareBy<Objetivo, LocalDate?>(nullsLast()) { it.dataAlvo }.thenBy { it.id })

    private fun progresso(objetivo: Objetivo, realizado: Long, hoje: LocalDate): ProgressoObjetivo {
        val falta = objetivo.valorAlvoCentavos - realizado
        val dataAlvo = objetivo.dataAlvo
        val prazoVencido = falta > 0 && dataAlvo != null && dataAlvo.isBefore(hoje)
        val porMes = if (falta > 0 && dataAlvo != null && !prazoVencido) {
            // Conta o mês atual: com a meta para este mês ainda dá para guardar uma vez.
            val meses = ChronoUnit.MONTHS.between(YearMonth.from(hoje), YearMonth.from(dataAlvo)) + 1
            (falta + meses - 1) / meses
        } else {
            null
        }
        return ProgressoObjetivo(objetivo, realizado, falta, porMes, prazoVencido)
    }
}

private fun fracao(parte: Long, total: Long): Double = if (total <= 0L) 0.0 else parte.toDouble() / total
