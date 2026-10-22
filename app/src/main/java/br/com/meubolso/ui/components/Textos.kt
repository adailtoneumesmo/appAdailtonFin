package br.com.meubolso.ui.components

import androidx.annotation.StringRes
import br.com.meubolso.R
import br.com.meubolso.domain.model.FormaPagamento
import br.com.meubolso.domain.model.Recorrencia
import br.com.meubolso.domain.model.TipoRenda
import br.com.meubolso.domain.model.VinculoPeriodo
import br.com.meubolso.domain.validacao.ErroValidacao

@get:StringRes
val ErroValidacao.mensagem: Int
    get() = when (this) {
        ErroValidacao.OBRIGATORIO -> R.string.erro_campo_obrigatorio
        ErroValidacao.VALOR_NAO_POSITIVO -> R.string.erro_valor_maior_zero
        ErroValidacao.DIA_FORA_DO_INTERVALO -> R.string.erro_dia_invalido
        ErroValidacao.DIA_UTIL_FORA_DO_INTERVALO -> R.string.erro_dia_util_invalido
        ErroValidacao.DATA_FIM_ANTES_DO_INICIO -> R.string.erro_data_fim_antes_inicio
        ErroValidacao.PARCELAS_FORA_DO_INTERVALO -> R.string.erro_parcelas_invalidas
        ErroValidacao.NOME_DUPLICADO -> R.string.erro_nome_duplicado
    }

@get:StringRes
val TipoRenda.rotulo: Int
    get() = when (this) {
        TipoRenda.SALARIO -> R.string.tipo_renda_salario
        TipoRenda.VALE -> R.string.tipo_renda_vale
        TipoRenda.RENDA_EXTRA -> R.string.tipo_renda_extra
    }

@get:StringRes
val FormaPagamento.rotulo: Int
    get() = when (this) {
        FormaPagamento.PIX -> R.string.forma_pix
        FormaPagamento.BOLETO -> R.string.forma_boleto
        FormaPagamento.DEBITO_AUTOMATICO -> R.string.forma_debito_automatico
        FormaPagamento.CARTAO_CREDITO -> R.string.forma_cartao_credito
    }

@get:StringRes
val VinculoPeriodo.rotulo: Int
    get() = when (this) {
        VinculoPeriodo.AUTOMATICO -> R.string.vinculo_automatico
        VinculoPeriodo.SALARIO -> R.string.vinculo_salario
        VinculoPeriodo.VALE -> R.string.vinculo_vale
    }

@get:StringRes
val Recorrencia.rotulo: Int
    get() = when (this) {
        Recorrencia.MENSAL -> R.string.recorrencia_mensal
        Recorrencia.PARCELADA -> R.string.recorrencia_parcelada
    }
