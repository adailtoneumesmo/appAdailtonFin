package br.com.meubolso.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import br.com.meubolso.util.Formatadores

/** Até R$ 9.999.999.999,99 — evita estouro e valores absurdos. */
private const val MAX_DIGITOS = 12

/**
 * Campo de valor monetário estilo "caixa eletrônico": os dígitos digitados entram
 * pela direita como centavos (1 → R$ 0,01; 123 → R$ 1,23).
 */
@Composable
fun CampoMoeda(
    valorCentavos: Long,
    onValorChange: (Long) -> Unit,
    rotulo: String,
    modifier: Modifier = Modifier,
    erro: String? = null,
) {
    val texto = if (valorCentavos == 0L) "" else Formatadores.moeda(valorCentavos)
    OutlinedTextField(
        value = TextFieldValue(texto, selection = TextRange(texto.length)),
        onValueChange = { novo ->
            val digitos = novo.text.filter(Char::isDigit).trimStart('0').take(MAX_DIGITOS)
            onValorChange(digitos.toLongOrNull() ?: 0L)
        },
        label = { Text(rotulo) },
        placeholder = { Text(Formatadores.moeda(0)) },
        singleLine = true,
        isError = erro != null,
        supportingText = erro?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        modifier = modifier,
    )
}
