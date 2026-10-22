package br.com.meubolso.ui.components

import androidx.annotation.StringRes
import br.com.meubolso.R
import br.com.meubolso.domain.model.TipoRenda
import br.com.meubolso.domain.validacao.ErroValidacao

@get:StringRes
val ErroValidacao.mensagem: Int
    get() = when (this) {
        ErroValidacao.OBRIGATORIO -> R.string.erro_campo_obrigatorio
        ErroValidacao.VALOR_NAO_POSITIVO -> R.string.erro_valor_maior_zero
        ErroValidacao.DIA_FORA_DO_INTERVALO -> R.string.erro_dia_invalido
        ErroValidacao.DIA_UTIL_FORA_DO_INTERVALO -> R.string.erro_dia_util_invalido
        ErroValidacao.DATA_FIM_ANTES_DO_INICIO -> R.string.erro_data_fim_antes_inicio
    }

@get:StringRes
val TipoRenda.rotulo: Int
    get() = when (this) {
        TipoRenda.SALARIO -> R.string.tipo_renda_salario
        TipoRenda.VALE -> R.string.tipo_renda_vale
        TipoRenda.RENDA_EXTRA -> R.string.tipo_renda_extra
    }
