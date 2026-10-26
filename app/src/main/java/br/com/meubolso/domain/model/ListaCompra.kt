package br.com.meubolso.domain.model

import java.time.LocalDate

data class ListaCompra(
    val id: Long = 0,
    val descricao: String,
    val data: LocalDate,
    /** Limite de gasto opcional. */
    val limiteCentavos: Long? = null,
    /** Despesa gerada ao finalizar (Pix, boleto, débito ou VR/VA). */
    val despesaId: Long? = null,
    /** Compra no cartão gerada ao finalizar com cartão de crédito. */
    val compraCartaoId: Long? = null,
) {
    /**
     * Finalizada enquanto a despesa gerada existir: excluir a despesa zera o vínculo
     * (ON DELETE SET NULL) e a lista pode ser finalizada de novo.
     */
    val finalizada: Boolean get() = despesaId != null || compraCartaoId != null
}

data class ItemListaCompra(
    val id: Long = 0,
    val listaId: Long,
    val nome: String,
    /** Preço unitário. */
    val valorCentavos: Long = 0,
    /** Quantidade × 1000, para aceitar frações (1,5 kg = 1500) sem usar Double. */
    val quantidadeMilesimos: Long = ItemListaCompra.QUANTIDADE_PADRAO,
    val noCarrinho: Boolean = false,
    /** Posição na lista (ordem de inclusão). */
    val ordem: Int = 0,
) {
    companion object {
        const val QUANTIDADE_PADRAO = 1_000L
    }
}
