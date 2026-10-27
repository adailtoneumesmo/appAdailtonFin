package br.com.meubolso.domain.objetivo

import br.com.meubolso.domain.model.Objetivo
import br.com.meubolso.domain.model.PosicaoInvestimentos
import br.com.meubolso.domain.validacao.ErroValidacao
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CalculadoraObjetivosTest {

    private val hoje = LocalDate.of(2026, 10, 27)

    private fun posicao(centavos: Long) = PosicaoInvestimentos(1, centavos, hoje)

    @Test
    fun `patrimonio vai primeiro para a data mais proxima e os sem data ficam por ultimo`() {
        val aposentadoria = Objetivo(1, "Aposentadoria", 1_000_000, dataAlvo = null)
        val viagem = Objetivo(2, "Viagem", 300_000, LocalDate.of(2027, 6, 1))
        val reserva = Objetivo(3, "Reserva", 500_000, LocalDate.of(2027, 1, 1))

        val resumo = CalculadoraObjetivos.resumir(listOf(aposentadoria, viagem, reserva), posicao(700_000), hoje)

        assertEquals(listOf("Reserva", "Viagem", "Aposentadoria"), resumo.itens.map { it.objetivo.nome })
        assertEquals(listOf(500_000L, 200_000L, 0L), resumo.itens.map { it.realizadoCentavos })
        assertEquals(1_800_000L, resumo.alvoCentavos)
        assertEquals(700_000L, resumo.realizadoCentavos)
        assertEquals(1_100_000L, resumo.faltaCentavos)
        assertTrue(resumo.itens[0].concluido)
    }

    @Test
    fun `patrimonio maior que a soma dos objetivos vira excedente`() {
        val resumo = CalculadoraObjetivos.resumir(listOf(Objetivo(1, "Carro", 100_000, null)), posicao(150_000), hoje)

        assertEquals(0L, resumo.faltaCentavos)
        assertEquals(50_000L, resumo.excedenteCentavos)
        assertEquals(1.0, resumo.fracao, 0.0)
    }

    @Test
    fun `sem posicao informada nada esta realizado`() {
        val resumo = CalculadoraObjetivos.resumir(listOf(Objetivo(1, "Carro", 100_000, null)), null, hoje)

        assertEquals(0L, resumo.realizadoCentavos)
        assertEquals(100_000L, resumo.faltaCentavos)
        assertEquals(0.0, resumo.fracao, 0.0)
    }

    @Test
    fun `valor por mes conta o mes atual e arredonda para cima`() {
        // Outubro a dezembro: 3 meses para R$ 1.000,00 → R$ 333,34.
        val objetivo = Objetivo(1, "Natal", 100_000, LocalDate.of(2026, 12, 20))
        val item = CalculadoraObjetivos.resumir(listOf(objetivo), null, hoje).itens.single()

        assertEquals(33_334L, item.porMesCentavos)
        assertFalse(item.prazoVencido)
    }

    @Test
    fun `data alvo no passado sem concluir e prazo vencido`() {
        val objetivo = Objetivo(1, "Curso", 100_000, LocalDate.of(2026, 9, 1))
        val item = CalculadoraObjetivos.resumir(listOf(objetivo), posicao(40_000), hoje).itens.single()

        assertTrue(item.prazoVencido)
        assertNull(item.porMesCentavos)
        assertEquals(60_000L, item.faltaCentavos)
    }

    @Test
    fun `validador exige nome e valor alvo positivo`() {
        assertEquals(
            mapOf(CampoObjetivo.NOME to ErroValidacao.OBRIGATORIO, CampoObjetivo.VALOR_ALVO to ErroValidacao.VALOR_NAO_POSITIVO),
            ValidadorObjetivo.validar(" ", 0),
        )
        assertTrue(ValidadorObjetivo.validar("Reserva", 1).isEmpty())
    }
}
