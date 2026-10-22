package br.com.meubolso.domain.renda

import br.com.meubolso.domain.calendario.CalendarioDiasUteis
import br.com.meubolso.domain.model.ModoDiaRecebimento
import br.com.meubolso.domain.model.Renda
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * @property nominal data pelo cadastro (dia fixo limitado ao fim do mês, ou N-ésimo dia útil).
 * @property prevista data efetiva, já antecipada para dia útil quando a regra estiver ligada.
 */
data class DataRecebimento(val nominal: LocalDate, val prevista: LocalDate) {
    val foiAntecipada: Boolean get() = nominal != prevista
}

class CalculadoraRecebimento @Inject constructor(
    private val calendario: CalendarioDiasUteis,
) {

    /**
     * Data de recebimento da renda no [mes], ou `null` se a renda não estiver vigente.
     * A vigência é verificada pela data nominal, para que uma antecipação não exclua
     * o primeiro recebimento. Não considera o flag [Renda.ativo].
     */
    fun dataNoMes(renda: Renda, mes: YearMonth, anteciparDiaNaoUtil: Boolean): DataRecebimento? {
        val nominal = when (renda.modoDia) {
            ModoDiaRecebimento.DIA_FIXO -> mes.atDay(minOf(renda.dia, mes.lengthOfMonth()))
            ModoDiaRecebimento.DIA_UTIL -> calendario.enesimoDiaUtil(mes, renda.dia)
        }
        if (!renda.vigenteEm(nominal)) return null
        val prevista = if (anteciparDiaNaoUtil) calendario.diaUtilAnteriorOuMesmo(nominal) else nominal
        return DataRecebimento(nominal, prevista)
    }

    /** Primeiro recebimento previsto em [hoje] ou depois, olhando até [mesesAFrente] meses. */
    fun proximoRecebimento(
        renda: Renda,
        hoje: LocalDate,
        anteciparDiaNaoUtil: Boolean,
        mesesAFrente: Long = 24,
    ): DataRecebimento? {
        val inicio = maxOf(YearMonth.from(hoje), YearMonth.from(renda.dataInicio))
        for (i in 0..mesesAFrente) {
            val data = dataNoMes(renda, inicio.plusMonths(i), anteciparDiaNaoUtil) ?: continue
            if (!data.prevista.isBefore(hoje)) return data
        }
        return null
    }
}
