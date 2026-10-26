package br.com.meubolso.domain.notificacao

import br.com.meubolso.domain.model.BancoMonitorado
import br.com.meubolso.domain.model.TipoTransacao

/**
 * Interpreta o texto das notificações dos bancos. Cada banco escreve de um jeito, então as
 * regras são genéricas: o que não for reconhecido fica em branco para o usuário completar.
 */
object LeitorNotificacaoBanco {

    val BANCOS = listOf(
        BancoMonitorado("Santander", listOf("santander")),
        BancoMonitorado("Itaú", listOf("itau")),
        BancoMonitorado("C6 Bank", listOf("c6bank")),
        BancoMonitorado("Swile", listOf("swile"), vale = true),
        BancoMonitorado("Nubank", listOf("com.nu.production")),
        BancoMonitorado("Inter", listOf("intermedium")),
        BancoMonitorado("Bradesco", listOf("bradesco")),
        BancoMonitorado("Banco do Brasil", listOf("br.com.bb.android")),
        BancoMonitorado("Caixa", listOf("br.com.gabba.caixa", "caixa")),
        BancoMonitorado("Mercado Pago", listOf("mercadopago")),
        BancoMonitorado("PicPay", listOf("picpay")),
    )

    /** "R$ 1.234,56", "R$45,90", "R$ 1234,56". */
    private val regexValor = Regex("""R\$\s*(\d{1,3}(?:\.\d{3})+|\d+),(\d{2})""")

    /** Estabelecimento após "em"/"no"/"na" (compra) ou destinatário após "para" (Pix). */
    private val regexEstabelecimento =
        Regex("""\b(?:em|no|na)\s+([A-Z0-9*][^\n,.;]{1,60}?)(?=\s+(?:em|no dia|às|as|foi|aprovad|realizad)\b|[,.;\n]|$)""")
    private val regexDestinatario =
        Regex("""\bpara\s+([^\n,.;]{2,60}?)(?=\s+(?:em|no dia|às|as|foi|realizad)\b|[,.;\n]|$)""", RegexOption.IGNORE_CASE)

    fun identificarBanco(pacote: String): BancoMonitorado? {
        val minusculo = pacote.lowercase()
        return BANCOS.firstOrNull { banco -> banco.trechosPacote.any { it in minusculo } }
    }

    /** Só vale guardar o que menciona dinheiro; o resto (avisos, login, propaganda sem valor) é ignorado. */
    fun mencionaValor(texto: String): Boolean = "R$" in texto

    /** Primeiro valor em reais do texto, em centavos. */
    fun extrairValor(texto: String): Long? {
        val achado = regexValor.find(texto) ?: return null
        val reais = achado.groupValues[1].replace(".", "").toLongOrNull() ?: return null
        return reais * 100 + achado.groupValues[2].toLong()
    }

    fun sugerirTipo(texto: String): TipoTransacao {
        val minusculo = texto.lowercase()
        return when {
            listOf("recebeu", "recebido", "recebida", "estorno", "devolvid", "reembolso", "cashback")
                .any { it in minusculo } -> TipoTransacao.ENTRADA
            "pix" in minusculo -> TipoTransacao.PIX
            listOf("compra", "cartão", "cartao", "crédito", "credito", "débito", "debito")
                .any { it in minusculo } -> TipoTransacao.CARTAO
            else -> TipoTransacao.OUTRO
        }
    }

    fun sugerirDescricao(texto: String, tipo: TipoTransacao): String? {
        val regex = if (tipo == TipoTransacao.PIX) regexDestinatario else regexEstabelecimento
        return regex.find(texto)?.groupValues?.get(1)?.trim()?.takeIf { it.length >= 2 }
    }
}
