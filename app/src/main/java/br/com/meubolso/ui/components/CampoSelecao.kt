package br.com.meubolso.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/** Ação extra exibida no fim do menu (ex.: "Gerenciar categorias…"). */
data class AcaoMenu(val rotulo: String, val onClick: () -> Unit)

/** Lista suspensa somente leitura. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> CampoSelecao(
    rotulo: String,
    opcoes: List<T>,
    selecionada: T?,
    textoDe: @Composable (T) -> String,
    onSelecionar: (T) -> Unit,
    modifier: Modifier = Modifier,
    erro: String? = null,
    iconeDe: (@Composable (T) -> Unit)? = null,
    acaoExtra: AcaoMenu? = null,
) {
    var expandido by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expandido,
        onExpandedChange = { expandido = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selecionada?.let { textoDe(it) }.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(rotulo) },
            leadingIcon = if (selecionada != null && iconeDe != null) {
                { iconeDe(selecionada) }
            } else {
                null
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            isError = erro != null,
            supportingText = erro?.let { { Text(it) } },
            singleLine = true,
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
            opcoes.forEach { opcao ->
                DropdownMenuItem(
                    text = { Text(textoDe(opcao)) },
                    leadingIcon = iconeDe?.let { { it(opcao) } },
                    onClick = {
                        onSelecionar(opcao)
                        expandido = false
                    },
                )
            }
            if (acaoExtra != null) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(acaoExtra.rotulo) },
                    onClick = {
                        expandido = false
                        acaoExtra.onClick()
                    },
                )
            }
        }
    }
}
