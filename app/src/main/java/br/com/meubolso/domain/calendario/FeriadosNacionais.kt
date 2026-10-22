package br.com.meubolso.domain.calendario

import java.time.LocalDate
import java.time.MonthDay

/**
 * Feriados nacionais oficiais do Brasil (Lei 662/1949, 6.802/1980 e 14.759/2023).
 * Pontos facultativos (Carnaval, Corpus Christi) não entram.
 */
object FeriadosNacionais {

    private val FIXOS = listOf(
        MonthDay.of(1, 1),   // Confraternização Universal
        MonthDay.of(4, 21),  // Tiradentes
        MonthDay.of(5, 1),   // Dia do Trabalho
        MonthDay.of(9, 7),   // Independência
        MonthDay.of(10, 12), // Nossa Senhora Aparecida
        MonthDay.of(11, 2),  // Finados
        MonthDay.of(11, 15), // Proclamação da República
        MonthDay.of(11, 20), // Consciência Negra (nacional desde 2024)
        MonthDay.of(12, 25), // Natal
    )

    fun doAno(ano: Int): Set<LocalDate> = buildSet {
        FIXOS.forEach { add(it.atYear(ano)) }
        add(pascoa(ano).minusDays(2)) // Sexta-feira Santa
    }

    /** Domingo de Páscoa pelo algoritmo de Meeus/Jones/Butcher (calendário gregoriano). */
    fun pascoa(ano: Int): LocalDate {
        val a = ano % 19
        val b = ano / 100
        val c = ano % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val mes = (h + l - 7 * m + 114) / 31
        val dia = (h + l - 7 * m + 114) % 31 + 1
        return LocalDate.of(ano, mes, dia)
    }
}
