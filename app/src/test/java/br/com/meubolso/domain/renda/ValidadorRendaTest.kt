package br.com.meubolso.domain.renda

import br.com.meubolso.domain.model.ModoDiaRecebimento
import br.com.meubolso.domain.validacao.ErroValidacao
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ValidadorRendaTest {

    private val hoje = LocalDate.of(2026, 10, 22)

    private fun validar(
        descricao: String = "Salário",
        valor: Long = 500_000,
        modo: ModoDiaRecebimento = ModoDiaRecebimento.DIA_FIXO,
        dia: Int? = 5,
        inicio: LocalDate? = hoje,
        fim: LocalDate? = null,
    ) = ValidadorRenda.validar(descricao, valor, modo, dia, inicio, fim)

    @Test
    fun `dados validos nao geram erros`() {
        assertTrue(validar().isEmpty())
        assertTrue(validar(modo = ModoDiaRecebimento.DIA_UTIL, dia = 23).isEmpty())
        assertTrue(validar(fim = hoje).isEmpty())
    }

    @Test
    fun `campos obrigatorios`() {
        val erros = validar(descricao = "  ", dia = null, inicio = null)
        assertEquals(ErroValidacao.OBRIGATORIO, erros[CampoRenda.DESCRICAO])
        assertEquals(ErroValidacao.OBRIGATORIO, erros[CampoRenda.DIA])
        assertEquals(ErroValidacao.OBRIGATORIO, erros[CampoRenda.DATA_INICIO])
    }

    @Test
    fun `valor deve ser maior que zero`() {
        assertEquals(ErroValidacao.VALOR_NAO_POSITIVO, validar(valor = 0)[CampoRenda.VALOR])
        assertEquals(ErroValidacao.VALOR_NAO_POSITIVO, validar(valor = -1)[CampoRenda.VALOR])
    }

    @Test
    fun `dia fixo entre 1 e 31`() {
        assertEquals(ErroValidacao.DIA_FORA_DO_INTERVALO, validar(dia = 0)[CampoRenda.DIA])
        assertEquals(ErroValidacao.DIA_FORA_DO_INTERVALO, validar(dia = 32)[CampoRenda.DIA])
        assertTrue(validar(dia = 31).isEmpty())
    }

    @Test
    fun `dia util entre 1 e 23`() {
        assertEquals(
            ErroValidacao.DIA_UTIL_FORA_DO_INTERVALO,
            validar(modo = ModoDiaRecebimento.DIA_UTIL, dia = 24)[CampoRenda.DIA],
        )
    }

    @Test
    fun `data de fim antes do inicio`() {
        assertEquals(
            ErroValidacao.DATA_FIM_ANTES_DO_INICIO,
            validar(fim = hoje.minusDays(1))[CampoRenda.DATA_FIM],
        )
    }
}
