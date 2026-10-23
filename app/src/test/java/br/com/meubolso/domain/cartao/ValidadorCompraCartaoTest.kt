package br.com.meubolso.domain.cartao

import br.com.meubolso.domain.despesa.CampoDespesa
import br.com.meubolso.domain.model.ModalidadeCompra
import br.com.meubolso.domain.validacao.ErroValidacao
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class ValidadorCompraCartaoTest {

    private fun validar(
        cartaoId: Long? = 1,
        descricao: String = "Mercado",
        categoriaId: Long? = 1,
        valor: Long = 10_000,
        modalidade: ModalidadeCompra = ModalidadeCompra.A_VISTA,
        parcelas: Int? = null,
        primeira: YearMonth? = YearMonth.of(2026, 3),
        ultima: YearMonth? = null,
    ) = ValidadorCompraCartao.validar(cartaoId, descricao, categoriaId, valor, modalidade, parcelas, primeira, ultima)

    @Test
    fun `compra a vista completa e valida`() {
        assertTrue(validar().isEmpty())
    }

    @Test
    fun `cartao, descricao, categoria e valor sao obrigatorios`() {
        val erros = validar(cartaoId = null, descricao = "", categoriaId = null, valor = 0)
        assertEquals(ErroValidacao.OBRIGATORIO, erros[CampoDespesa.CARTAO])
        assertEquals(ErroValidacao.OBRIGATORIO, erros[CampoDespesa.DESCRICAO])
        assertEquals(ErroValidacao.OBRIGATORIO, erros[CampoDespesa.CATEGORIA])
        assertEquals(ErroValidacao.VALOR_NAO_POSITIVO, erros[CampoDespesa.VALOR])
    }

    @Test
    fun `parcelado exige quantidade de parcelas valida`() {
        val parcelado = ModalidadeCompra.PARCELADO
        assertEquals(ErroValidacao.OBRIGATORIO, validar(modalidade = parcelado)[CampoDespesa.PARCELAS])
        assertEquals(ErroValidacao.PARCELAS_FORA_DO_INTERVALO, validar(modalidade = parcelado, parcelas = 1)[CampoDespesa.PARCELAS])
        assertTrue(validar(modalidade = parcelado, parcelas = 5).isEmpty())
    }

    @Test
    fun `parcelado exige pelo menos um centavo por parcela`() {
        val erros = validar(modalidade = ModalidadeCompra.PARCELADO, parcelas = 3, valor = 2)
        assertEquals(ErroValidacao.VALOR_MENOR_QUE_PARCELAS, erros[CampoDespesa.VALOR])
        assertTrue(validar(modalidade = ModalidadeCompra.PARCELADO, parcelas = 3, valor = 3).isEmpty())
    }

    @Test
    fun `parcelas sao ignoradas fora do parcelado`() {
        assertTrue(validar(modalidade = ModalidadeCompra.A_VISTA, parcelas = 1).isEmpty())
    }

    @Test
    fun `fixa nao pode encerrar antes da primeira fatura`() {
        val fixo = ModalidadeCompra.FIXO
        assertEquals(
            ErroValidacao.DATA_FIM_ANTES_DO_INICIO,
            validar(modalidade = fixo, ultima = YearMonth.of(2026, 2))[CampoDespesa.ENCERRAMENTO],
        )
        assertTrue(validar(modalidade = fixo, ultima = YearMonth.of(2026, 3)).isEmpty())
        assertTrue(validar(modalidade = fixo, ultima = null).isEmpty())
    }
}
