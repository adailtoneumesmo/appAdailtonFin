package br.com.meubolso.ui.despesas

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
import br.com.meubolso.domain.despesa.CampoDespesa
import br.com.meubolso.domain.model.FormaPagamento
import br.com.meubolso.domain.model.Recorrencia
import br.com.meubolso.domain.model.VinculoPeriodo
import br.com.meubolso.ui.components.AcaoMenu
import br.com.meubolso.ui.components.CampoMoeda
import br.com.meubolso.ui.components.CampoSelecao
import br.com.meubolso.ui.components.PontoCor
import br.com.meubolso.ui.components.SeletorMes
import br.com.meubolso.ui.components.mensagem
import br.com.meubolso.ui.components.rotulo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DespesaFormScreen(
    onVoltar: () -> Unit,
    onGerenciarCategorias: () -> Unit,
    viewModel: DespesaFormViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val categorias by viewModel.categorias.collectAsStateWithLifecycle()
    var confirmarExclusao by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(estado.concluido) {
        if (estado.concluido) onVoltar()
    }

    @Composable
    fun erroDe(campo: CampoDespesa): String? = estado.erros[campo]?.let { stringResource(it.mensagem) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (estado.editando) R.string.despesa_form_titulo_editar else R.string.despesa_form_titulo_nova,
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
                label = { Text(stringResource(R.string.despesa_campo_descricao)) },
                placeholder = { Text(stringResource(R.string.despesa_campo_descricao_dica)) },
                singleLine = true,
                isError = estado.erros.containsKey(CampoDespesa.DESCRICAO),
                supportingText = erroDe(CampoDespesa.DESCRICAO)?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            CampoSelecao(
                rotulo = stringResource(R.string.despesa_campo_categoria),
                opcoes = categorias,
                selecionada = categorias.firstOrNull { it.id == estado.categoriaId },
                textoDe = { it.nome },
                onSelecionar = viewModel::alterarCategoria,
                iconeDe = { PontoCor(it.cor) },
                erro = erroDe(CampoDespesa.CATEGORIA),
                acaoExtra = AcaoMenu(stringResource(R.string.despesa_gerenciar_categorias), onGerenciarCategorias),
                modifier = Modifier.fillMaxWidth(),
            )

            CampoMoeda(
                valorCentavos = estado.valorCentavos,
                onValorChange = viewModel::alterarValor,
                rotulo = stringResource(R.string.despesa_campo_valor),
                erro = erroDe(CampoDespesa.VALOR),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = estado.dia,
                onValueChange = viewModel::alterarDia,
                label = { Text(stringResource(R.string.despesa_campo_dia)) },
                singleLine = true,
                isError = estado.erros.containsKey(CampoDespesa.DIA),
                supportingText = {
                    Text(erroDe(CampoDespesa.DIA) ?: stringResource(R.string.despesa_campo_dia_ajuda))
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )

            CampoSelecao(
                rotulo = stringResource(R.string.despesa_campo_forma),
                opcoes = FormaPagamento.entries,
                selecionada = estado.formaPagamento,
                textoDe = { stringResource(it.rotulo) },
                onSelecionar = viewModel::alterarFormaPagamento,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(stringResource(R.string.despesa_campo_vinculo), style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                VinculoPeriodo.entries.forEachIndexed { indice, vinculo ->
                    SegmentedButton(
                        selected = estado.vinculo == vinculo,
                        onClick = { viewModel.alterarVinculo(vinculo) },
                        shape = SegmentedButtonDefaults.itemShape(indice, VinculoPeriodo.entries.size),
                    ) { Text(stringResource(vinculo.rotulo)) }
                }
            }
            if (estado.vinculo == VinculoPeriodo.AUTOMATICO) {
                Text(
                    stringResource(R.string.despesa_vinculo_ajuda),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(stringResource(R.string.despesa_campo_recorrencia), style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                Recorrencia.entries.forEachIndexed { indice, recorrencia ->
                    SegmentedButton(
                        selected = estado.recorrencia == recorrencia,
                        onClick = { viewModel.alterarRecorrencia(recorrencia) },
                        shape = SegmentedButtonDefaults.itemShape(indice, Recorrencia.entries.size),
                    ) { Text(stringResource(recorrencia.rotulo)) }
                }
            }
            if (estado.recorrencia == Recorrencia.PARCELADA) {
                OutlinedTextField(
                    value = estado.totalParcelas,
                    onValueChange = viewModel::alterarTotalParcelas,
                    label = { Text(stringResource(R.string.despesa_campo_parcelas)) },
                    singleLine = true,
                    isError = estado.erros.containsKey(CampoDespesa.PARCELAS),
                    supportingText = erroDe(CampoDespesa.PARCELAS)?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Text(
                stringResource(
                    if (estado.recorrencia == Recorrencia.PARCELADA) {
                        R.string.despesa_campo_mes_inicio_parcelada
                    } else {
                        R.string.despesa_campo_mes_inicio
                    },
                ),
                style = MaterialTheme.typography.labelLarge,
            )
            SeletorMes(
                mes = estado.mesInicio,
                onMesChange = viewModel::alterarMesInicio,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.despesa_campo_ativa),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = estado.ativa, onCheckedChange = viewModel::alterarAtiva)
            }

            Button(onClick = viewModel::salvar, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.acao_salvar))
            }
        }
    }

    if (confirmarExclusao) {
        AlertDialog(
            onDismissRequest = { confirmarExclusao = false },
            title = { Text(stringResource(R.string.despesa_excluir_titulo)) },
            text = { Text(stringResource(R.string.despesa_excluir_mensagem, estado.descricao)) },
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
