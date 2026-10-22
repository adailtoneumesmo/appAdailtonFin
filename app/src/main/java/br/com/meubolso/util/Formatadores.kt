package br.com.meubolso.util

import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object Formatadores {

    val LOCALE_BR: Locale = Locale.forLanguageTag("pt-BR")

    private val formatoData = DateTimeFormatter.ofPattern("dd/MM/yyyy", LOCALE_BR)

    /** 123456 → "R$ 1.234,56". NumberFormat não é thread-safe, por isso uma instância por chamada. */
    fun moeda(centavos: Long): String =
        NumberFormat.getCurrencyInstance(LOCALE_BR).format(BigDecimal.valueOf(centavos, 2))

    fun data(data: LocalDate): String = formatoData.format(data)
}
