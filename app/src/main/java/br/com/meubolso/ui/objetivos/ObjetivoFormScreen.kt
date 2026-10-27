package br.com.meubolso.ui.objetivos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.meubolso.R
import br.com.meubolso.domain.objetivo.CampoObjetivo
import br.com.meubolso.ui.components.CampoData
import br.com.meubolso.ui.components.CampoMoeda
import br.com.meubolso.ui.components.mensagem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObjetivoFormScreen(
    onVoltar: () -> Unit,
    viewModel: ObjetivoFormViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(estado.concluido) {
        if (estado.concluido) onVoltar()
    }

    @Composable
    fun erroDe(campo: CampoObjetivo): String? = estado.erros[campo]?.let { stringResource(it.mensagem) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (estado.editando) R.string.objetivo_form_titulo_editar else R.string.objetivo_form_titulo_novo,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.acao_voltar))
                    }
                },
                actions = {
                    if (estado.editando) {
                        IconButton(onClick = viewModel::pedirExclusao) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.acao_excluir))
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            OutlinedTextField(
                value = estado.nome,
                onValueChange = viewModel::alterarNome,
                label = { Text(stringResource(R.string.objetivo_campo_nome)) },
                placeholder = { Text(stringResource(R.string.objetivo_campo_nome_dica)) },
                singleLine = true,
                isError = estado.erros.containsKey(CampoObjetivo.NOME),
                supportingText = erroDe(CampoObjetivo.NOME)?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            CampoMoeda(
                valorCentavos = estado.valorAlvoCentavos,
                onValorChange = viewModel::alterarValorAlvo,
                rotulo = stringResource(R.string.objetivo_campo_valor_alvo),
                erro = erroDe(CampoObjetivo.VALOR_ALVO),
                modifier = Modifier.fillMaxWidth(),
            )
            CampoData(
                rotulo = stringResource(R.string.objetivo_campo_data_alvo),
                data = estado.dataAlvo,
                onDataChange = viewModel::alterarDataAlvo,
                textoVazio = stringResource(R.string.objetivo_campo_sem_data),
                permiteLimpar = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = viewModel::salvar, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.acao_salvar))
            }
        }
    }

    if (estado.confirmarExclusao) {
        AlertDialog(
            onDismissRequest = viewModel::cancelarExclusao,
            title = { Text(stringResource(R.string.objetivo_excluir_titulo)) },
            text = { Text(stringResource(R.string.objetivo_excluir_mensagem, estado.nome)) },
            confirmButton = {
                TextButton(onClick = viewModel::excluir) { Text(stringResource(R.string.acao_excluir)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelarExclusao) { Text(stringResource(R.string.acao_cancelar)) }
            },
        )
    }
}
