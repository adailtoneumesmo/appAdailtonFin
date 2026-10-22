package br.com.meubolso.ui.rendas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.meubolso.R
import br.com.meubolso.domain.model.ModoDiaRecebimento
import br.com.meubolso.domain.model.TipoRenda
import br.com.meubolso.domain.renda.CampoRenda
import br.com.meubolso.domain.renda.DataRecebimento
import br.com.meubolso.ui.components.CampoData
import br.com.meubolso.ui.components.CampoMoeda
import br.com.meubolso.ui.components.mensagem
import br.com.meubolso.ui.components.rotulo
import br.com.meubolso.util.Formatadores

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RendaFormScreen(
    onVoltar: () -> Unit,
    viewModel: RendaFormViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val previsao by viewModel.previsao.collectAsStateWithLifecycle()
    var confirmarExclusao by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(estado.concluido) {
        if (estado.concluido) onVoltar()
    }

    @Composable
    fun erroDe(campo: CampoRenda): String? = estado.erros[campo]?.let { stringResource(it.mensagem) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (estado.editando) R.string.renda_form_titulo_editar else R.string.renda_form_titulo_nova,
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
                        IconButton(onClick = { confirmarExclusao = true }) {
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
                value = estado.descricao,
                onValueChange = viewModel::alterarDescricao,
                label = { Text(stringResource(R.string.renda_campo_descricao)) },
                placeholder = { Text(stringResource(R.string.renda_campo_descricao_dica)) },
                singleLine = true,
                isError = estado.erros.containsKey(CampoRenda.DESCRICAO),
                supportingText = erroDe(CampoRenda.DESCRICAO)?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Text(stringResource(R.string.renda_campo_tipo), style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                TipoRenda.entries.forEachIndexed { indice, tipo ->
                    SegmentedButton(
                        selected = estado.tipo == tipo,
                        onClick = { viewModel.alterarTipo(tipo) },
                        shape = SegmentedButtonDefaults.itemShape(indice, TipoRenda.entries.size),
                    ) { Text(stringResource(tipo.rotulo)) }
                }
            }

            CampoMoeda(
                valorCentavos = estado.valorCentavos,
                onValorChange = viewModel::alterarValor,
                rotulo = stringResource(R.string.renda_campo_valor),
                erro = erroDe(CampoRenda.VALOR),
                modifier = Modifier.fillMaxWidth(),
            )

            Text(stringResource(R.string.renda_campo_modo_dia), style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                ModoDiaRecebimento.entries.forEachIndexed { indice, modo ->
                    SegmentedButton(
                        selected = estado.modoDia == modo,
                        onClick = { viewModel.alterarModoDia(modo) },
                        shape = SegmentedButtonDefaults.itemShape(indice, ModoDiaRecebimento.entries.size),
                    ) {
                        Text(
                            stringResource(
                                when (modo) {
                                    ModoDiaRecebimento.DIA_FIXO -> R.string.renda_modo_dia_fixo
                                    ModoDiaRecebimento.DIA_UTIL -> R.string.renda_modo_dia_util
                                },
                            ),
                        )
                    }
                }
            }

            OutlinedTextField(
                value = estado.dia,
                onValueChange = viewModel::alterarDia,
                label = {
                    Text(
                        stringResource(
                            if (estado.modoDia == ModoDiaRecebimento.DIA_UTIL) {
                                R.string.renda_campo_dia_util
                            } else {
                                R.string.renda_campo_dia
                            },
                        ),
                    )
                },
                singleLine = true,
                isError = estado.erros.containsKey(CampoRenda.DIA),
                supportingText = erroDe(CampoRenda.DIA)?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )

            CampoData(
                rotulo = stringResource(R.string.renda_campo_data_inicio),
                data = estado.dataInicio,
                onDataChange = viewModel::alterarDataInicio,
                erro = erroDe(CampoRenda.DATA_INICIO),
                modifier = Modifier.fillMaxWidth(),
            )

            CampoData(
                rotulo = stringResource(R.string.renda_campo_data_fim),
                data = estado.dataFim,
                onDataChange = viewModel::alterarDataFim,
                erro = erroDe(CampoRenda.DATA_FIM),
                textoVazio = stringResource(R.string.renda_sem_data_fim),
                permiteLimpar = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.renda_campo_ativo),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = estado.ativo, onCheckedChange = viewModel::alterarAtivo)
            }

            previsao?.let { CardPrevisao(it) }

            Button(onClick = viewModel::salvar, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.acao_salvar))
            }
        }
    }

    if (confirmarExclusao) {
        AlertDialog(
            onDismissRequest = { confirmarExclusao = false },
            title = { Text(stringResource(R.string.renda_excluir_titulo)) },
            text = { Text(stringResource(R.string.renda_excluir_mensagem, estado.descricao)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmarExclusao = false
                    viewModel.excluir()
                }) { Text(stringResource(R.string.acao_excluir)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmarExclusao = false }) {
                    Text(stringResource(R.string.acao_cancelar))
                }
            },
        )
    }
}

@Composable
private fun CardPrevisao(previsao: DataRecebimento) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.renda_previsao, Formatadores.data(previsao.prevista)),
                style = MaterialTheme.typography.bodyLarge,
            )
            if (previsao.foiAntecipada) {
                Text(
                    stringResource(R.string.renda_previsao_ajustada, Formatadores.data(previsao.nominal)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
