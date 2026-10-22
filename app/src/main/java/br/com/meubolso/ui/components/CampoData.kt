package br.com.meubolso.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import br.com.meubolso.R
import br.com.meubolso.util.Formatadores
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Campo somente leitura que abre um DatePicker ao ser tocado. */
@Composable
fun CampoData(
    rotulo: String,
    data: LocalDate?,
    onDataChange: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    erro: String? = null,
    textoVazio: String? = null,
    permiteLimpar: Boolean = false,
) {
    var mostrarSeletor by rememberSaveable { mutableStateOf(false) }
    val interacoes = remember { MutableInteractionSource() }
    LaunchedEffect(interacoes) {
        interacoes.interactions.collect { if (it is PressInteraction.Release) mostrarSeletor = true }
    }

    OutlinedTextField(
        value = data?.let(Formatadores::data).orEmpty(),
        onValueChange = {},
        readOnly = true,
        label = { Text(rotulo) },
        placeholder = textoVazio?.let { { Text(it) } },
        trailingIcon = {
            if (permiteLimpar && data != null) {
                IconButton(onClick = { onDataChange(null) }) {
                    Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.acao_limpar))
                }
            } else {
                Icon(Icons.Default.DateRange, contentDescription = null)
            }
        },
        isError = erro != null,
        supportingText = erro?.let { { Text(it) } },
        singleLine = true,
        interactionSource = interacoes,
        modifier = modifier,
    )

    if (mostrarSeletor) {
        // O DatePicker trabalha com milissegundos em UTC.
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = data?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { mostrarSeletor = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { millis ->
                        onDataChange(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    mostrarSeletor = false
                }) { Text(stringResource(R.string.acao_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarSeletor = false }) {
                    Text(stringResource(R.string.acao_cancelar))
                }
            },
        ) {
            DatePicker(state = estado)
        }
    }
}
