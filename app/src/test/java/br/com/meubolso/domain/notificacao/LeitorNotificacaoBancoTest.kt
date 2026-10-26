package br.com.meubolso.domain.notificacao

import br.com.meubolso.domain.model.TipoTransacao
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LeitorNotificacaoBancoTest {

    @Test
    fun `identifica banco pelo pacote`() {
        assertEquals("Santander", LeitorNotificacaoBanco.identificarBanco("com.santander.app")?.nome)
        assertEquals("Itaú", LeitorNotificacaoBanco.identificarBanco("com.itau")?.nome)
        assertEquals("C6 Bank", LeitorNotificacaoBanco.identificarBanco("com.c6bank.app")?.nome)
        assertTrue(LeitorNotificacaoBanco.identificarBanco("br.com.swile.app")!!.vale)
        assertNull(LeitorNotificacaoBanco.identificarBanco("com.whatsapp"))
    }

    @Test
    fun `extrai valor em reais`() {
        assertEquals(4_590L, LeitorNotificacaoBanco.extrairValor("Compra de R$ 45,90 aprovada"))
        assertEquals(123_456L, LeitorNotificacaoBanco.extrairValor("Pix de R$1.234,56 enviado"))
        assertEquals(123_456L, LeitorNotificacaoBanco.extrairValor("valor R$ 1234,56"))
        assertEquals(100_000_000L, LeitorNotificacaoBanco.extrairValor("R$ 1.000.000,00"))
        assertNull(LeitorNotificacaoBanco.extrairValor("Seu cartão chegou"))
    }

    @Test
    fun `so guarda o que menciona dinheiro`() {
        assertTrue(LeitorNotificacaoBanco.mencionaValor("Compra de R$ 10,00"))
        assertFalse(LeitorNotificacaoBanco.mencionaValor("Novo acesso à sua conta"))
    }

    @Test
    fun `sugere o tipo pela mensagem`() {
        assertEquals(TipoTransacao.PIX, LeitorNotificacaoBanco.sugerirTipo("Pix enviado de R$ 100,00 para Fulano"))
        assertEquals(TipoTransacao.CARTAO, LeitorNotificacaoBanco.sugerirTipo("Compra aprovada no cartão final 1234"))
        assertEquals(TipoTransacao.ENTRADA, LeitorNotificacaoBanco.sugerirTipo("Você recebeu um Pix de R$ 50,00"))
        assertEquals(TipoTransacao.OUTRO, LeitorNotificacaoBanco.sugerirTipo("Transação de R$ 10,00"))
    }

    @Test
    fun `sugere estabelecimento da compra`() {
        val texto = "Compra aprovada no cartão final 1234 de R$ 45,90 em PADARIA PAO QUENTE."
        assertEquals("PADARIA PAO QUENTE", LeitorNotificacaoBanco.sugerirDescricao(texto, TipoTransacao.CARTAO))
    }

    @Test
    fun `sugere destinatario do pix`() {
        val texto = "Pix enviado de R$ 100,00 para Maria da Silva em 26/10"
        assertEquals("Maria da Silva", LeitorNotificacaoBanco.sugerirDescricao(texto, TipoTransacao.PIX))
    }

    @Test
    fun `sem estabelecimento nao sugere descricao`() {
        assertNull(LeitorNotificacaoBanco.sugerirDescricao("Compra de R$ 10,00 aprovada", TipoTransacao.CARTAO))
    }
}
