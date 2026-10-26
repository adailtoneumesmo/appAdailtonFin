package br.com.meubolso.domain.calendario

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dia útil = segunda a sexta, exceto os feriados das [fontes].
 * Por padrão só os feriados nacionais; pontos facultativos não contam.
 */
@Singleton
class CalendarioDiasUteis(private val fontes: List<FonteFeriados>) {

    @Inject
    constructor() : this(listOf(FeriadosNacionais))

    private val feriadosPorAno = ConcurrentHashMap<Int, Set<LocalDate>>()

    fun ehFeriado(data: LocalDate): Boolean =
        data in feriadosPorAno.getOrPut(data.year) { fontes.flatMapTo(HashSet()) { it.doAno(data.year) } }

    fun ehDiaUtil(data: LocalDate): Boolean =
        data.dayOfWeek != DayOfWeek.SATURDAY &&
            data.dayOfWeek != DayOfWeek.SUNDAY &&
            !ehFeriado(data)

    /** Retorna a própria data se for dia útil; senão, o dia útil imediatamente anterior. */
    fun diaUtilAnteriorOuMesmo(data: LocalDate): LocalDate {
        var atual = data
        while (!ehDiaUtil(atual)) atual = atual.minusDays(1)
        return atual
    }

    fun quantidadeNoMes(mes: YearMonth): Int = (1..mes.lengthOfMonth()).count { ehDiaUtil(mes.atDay(it)) }

    /**
     * N-ésimo dia útil do mês. Se o mês tiver menos de [n] dias úteis,
     * retorna o último dia útil do mês.
     */
    fun enesimoDiaUtil(mes: YearMonth, n: Int): LocalDate {
        require(n >= 1) { "n deve ser maior ou igual a 1" }
        var contador = 0
        var ultimoUtil: LocalDate? = null
        for (dia in 1..mes.lengthOfMonth()) {
            val data = mes.atDay(dia)
            if (ehDiaUtil(data)) {
                contador++
                ultimoUtil = data
                if (contador == n) return data
            }
        }
        return checkNotNull(ultimoUtil) { "Mês sem dias úteis: $mes" }
    }
}
