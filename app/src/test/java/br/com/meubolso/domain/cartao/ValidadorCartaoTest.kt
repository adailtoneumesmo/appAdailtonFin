package br.com.meubolso.domain.cartao

import br.com.meubolso.domain.model.Origem
import br.com.meubolso.domain.validacao.ErroValidacao
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidadorCartaoTest {

    @Test
    fun `cartao completo e valido`() {
        assertTrue(ValidadorCartao.validar("Nubank", 10, 17, Origem.VALE).isEmpty())
    }

    @Test
    fun `todos os campos sao obrigatorios`() {
        val erros = ValidadorCartao.validar(" ", null, null, null)
        assertEquals(
            mapOf(
                CampoCartao.NOME to ErroValidacao.OBRIGATORIO,
                CampoCartao.FECHAMENTO to ErroValidacao.OBRIGATORIO,
                CampoCartao.VENCIMENTO to ErroValidacao.OBRIGATORIO,
                CampoCartao.FONTE to ErroValidacao.OBRIGATORIO,
            ),
            erros,
        )
    }

    @Test
    fun `dias devem estar entre 1 e 31`() {
        val erros = ValidadorCartao.validar("Itaú", 0, 32, Origem.SALARIO)
        assertEquals(ErroValidacao.DIA_FORA_DO_INTERVALO, erros[CampoCartao.FECHAMENTO])
        assertEquals(ErroValidacao.DIA_FORA_DO_INTERVALO, erros[CampoCartao.VENCIMENTO])
        assertTrue(ValidadorCartao.validar("Itaú", 1, 31, Origem.SALARIO).isEmpty())
    }
}
