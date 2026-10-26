package br.com.meubolso.domain.listacompra

import br.com.meubolso.domain.validacao.ErroValidacao
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ValidadorListaCompraTest {

    @Test
    fun `lista exige descricao e data`() {
        val erros = ValidadorListaCompra.validarLista("  ", null)
        assertEquals(ErroValidacao.OBRIGATORIO, erros[CampoListaCompra.DESCRICAO])
        assertEquals(ErroValidacao.OBRIGATORIO, erros[CampoListaCompra.DATA])
        assertTrue(ValidadorListaCompra.validarLista("Mercado", LocalDate.of(2026, 10, 26)).isEmpty())
    }

    @Test
    fun `item exige nome e quantidade valida`() {
        assertEquals(ErroValidacao.OBRIGATORIO, ValidadorListaCompra.validarItem("", "1")[CampoListaCompra.NOME_ITEM])
        assertEquals(
            ErroValidacao.QUANTIDADE_INVALIDA,
            ValidadorListaCompra.validarItem("Arroz", "1,2,3")[CampoListaCompra.QUANTIDADE],
        )
        assertTrue(ValidadorListaCompra.validarItem("Arroz", "").isEmpty())
        assertTrue(ValidadorListaCompra.validarItem("Banana", "1,5").isEmpty())
    }
}
