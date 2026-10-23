package br.com.meubolso.domain.cartao

import br.com.meubolso.domain.despesa.CampoDespesa
import br.com.meubolso.domain.despesa.ValidadorDespesa
import br.com.meubolso.domain.model.ModalidadeCompra
import br.com.meubolso.domain.validacao.ErroValidacao
import java.time.YearMonth

/** Usa [CampoDespesa] porque a compra é preenchida no mesmo formulário da despesa. */
object ValidadorCompraCartao {

    fun validar(
        cartaoId: Long?,
        descricao: String,
        categoriaId: Long?,
        valorCentavos: Long,
        modalidade: ModalidadeCompra,
        totalParcelas: Int?,
        mesPrimeiraFatura: YearMonth?,
        mesUltimaFatura: YearMonth?,
    ): Map<CampoDespesa, ErroValidacao> = buildMap {
        if (cartaoId == null) put(CampoDespesa.CARTAO, ErroValidacao.OBRIGATORIO)

        if (descricao.isBlank()) put(CampoDespesa.DESCRICAO, ErroValidacao.OBRIGATORIO)

        if (categoriaId == null) put(CampoDespesa.CATEGORIA, ErroValidacao.OBRIGATORIO)

        if (valorCentavos <= 0) put(CampoDespesa.VALOR, ErroValidacao.VALOR_NAO_POSITIVO)

        if (modalidade == ModalidadeCompra.PARCELADO) {
            when {
                totalParcelas == null -> put(CampoDespesa.PARCELAS, ErroValidacao.OBRIGATORIO)
                totalParcelas !in ValidadorDespesa.INTERVALO_PARCELAS ->
                    put(CampoDespesa.PARCELAS, ErroValidacao.PARCELAS_FORA_DO_INTERVALO)
                // Cada parcela precisa de pelo menos R$ 0,01.
                valorCentavos in 1 until totalParcelas ->
                    put(CampoDespesa.VALOR, ErroValidacao.VALOR_MENOR_QUE_PARCELAS)
            }
        }

        if (modalidade == ModalidadeCompra.FIXO && mesUltimaFatura != null && mesPrimeiraFatura != null &&
            mesUltimaFatura < mesPrimeiraFatura
        ) {
            put(CampoDespesa.ENCERRAMENTO, ErroValidacao.DATA_FIM_ANTES_DO_INICIO)
        }
    }
}
