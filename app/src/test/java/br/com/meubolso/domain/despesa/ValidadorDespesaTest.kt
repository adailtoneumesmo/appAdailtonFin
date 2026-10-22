package br.com.meubolso.domain.despesa

import br.com.meubolso.domain.model.Recorrencia
import br.com.meubolso.domain.validacao.ErroValidacao
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidadorDespesaTest {

    private fun validar(
        descricao: String = "Aluguel",
        categoriaId: Long? = 1,
        valor: Long = 150_000,
        dia: Int? = 10,
        recorrencia: Recorrencia = Recorrencia.MENSAL,
        parcelas: Int? = null,
    ) = ValidadorDespesa.validar(descricao, categoriaId, valor, dia, recorrencia, parcelas)

    @Test
    fun `dados validos nao geram erros`() {
        assertTrue(validar().isEmpty())
        assertTrue(validar(dia = 31).isEmpty())
        assertTrue(validar(recorrencia = Recorrencia.PARCELADA, parcelas = 12).isEmpty())
    }

    @Test
    fun `campos obrigatorios`() {
        val erros = validar(descricao = "", categoriaId = null, dia = null)
        assertEquals(ErroValidacao.OBRIGATORIO, erros[CampoDespesa.DESCRICAO])
        assertEquals(ErroValidacao.OBRIGATORIO, erros[CampoDespesa.CATEGORIA])
        assertEquals(ErroValidacao.OBRIGATORIO, erros[CampoDespesa.DIA])
    }

    @Test
    fun `valor maior que zero e dia entre 1 e 31`() {
        assertEquals(ErroValidacao.VALOR_NAO_POSITIVO, validar(valor = 0)[CampoDespesa.VALOR])
        assertEquals(ErroValidacao.DIA_FORA_DO_INTERVALO, validar(dia = 0)[CampoDespesa.DIA])
        assertEquals(ErroValidacao.DIA_FORA_DO_INTERVALO, validar(dia = 32)[CampoDespesa.DIA])
    }

    @Test
    fun `parcelada exige numero de parcelas valido`() {
        assertEquals(
            ErroValidacao.OBRIGATORIO,
            validar(recorrencia = Recorrencia.PARCELADA)[CampoDespesa.PARCELAS],
        )
        assertEquals(
            ErroValidacao.PARCELAS_FORA_DO_INTERVALO,
            validar(recorrencia = Recorrencia.PARCELADA, parcelas = 1)[CampoDespesa.PARCELAS],
        )
        // Mensal ignora o campo de parcelas.
        assertTrue(validar(recorrencia = Recorrencia.MENSAL, parcelas = 1).isEmpty())
    }
}
