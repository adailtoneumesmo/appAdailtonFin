package br.com.meubolso.domain.calendario

import java.time.LocalDate

/**
 * Uma lista de feriados que deixam de ser dia útil. Hoje só [FeriadosNacionais] é usada;
 * feriados estaduais/municipais ou pontos facultativos (Carnaval, Corpus Christi) entram
 * como novas fontes passadas ao [CalendarioDiasUteis].
 */
fun interface FonteFeriados {
    fun doAno(ano: Int): Set<LocalDate>
}
