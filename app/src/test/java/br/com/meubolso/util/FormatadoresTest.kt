package br.com.meubolso.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class FormatadoresTest {

    /** NumberFormat pt-BR usa espaço não separável entre "R$" e o valor. */
    private fun normalizar(texto: String) = texto.replace('\u00A0', ' ')

    @Test
    fun `formata centavos em reais`() {
        assertEquals("R$ 1.234,56", normalizar(Formatadores.moeda(123_456)))
        assertEquals("R$ 0,05", normalizar(Formatadores.moeda(5)))
        assertEquals("R$ 0,00", normalizar(Formatadores.moeda(0)))
    }

    @Test
    fun `formata data dd-MM-yyyy`() {
        assertEquals("05/01/2027", Formatadores.data(LocalDate.of(2027, 1, 5)))
    }

    @Test
    fun `formata mes e ano com inicial maiuscula`() {
        assertEquals("Outubro 2026", Formatadores.mesAno(YearMonth.of(2026, 10)))
        assertEquals("Março 2027", Formatadores.mesAno(YearMonth.of(2027, 3)))
    }
}
