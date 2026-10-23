package br.com.meubolso.ui.cartoes

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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.meubolso.R
import br.com.meubolso.domain.cartao.CampoCartao
import br.com.meubolso.domain.model.Origem
import br.com.meubolso.ui.components.mensagem
import br.com.meubolso.ui.components.rotulo

private val FONTES = listOf(Origem.VALE, Origem.SALARIO)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartaoFormScreen(
    onVoltar: () -> Unit,
    viewModel: CartaoFormViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(estado.concluido) {
        if (estado.concluido) onVoltar()
    }

    @Composable
    fun erroDe(campo: CampoCartao): String? = estado.erros[campo]?.let { stringResource(it.mensagem) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (estado.editando) R.string.cartao_form_titulo_editar else R.string.cartao_form_titulo_novo,
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
                label = { Text(stringResource(R.string.cartao_campo_nome)) },
                placeholder = { Text(stringResource(R.string.cartao_campo_nome_dica)) },
                singleLine = true,
                isError = estado.erros.containsKey(CampoCartao.NOME),
                supportingText = erroDe(CampoCartao.NOME)?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                CampoDia(
                    valor = estado.diaFechamento,
                    onValorChange = viewModel::alterarDiaFechamento,
                    rotulo = stringResource(R.string.cartao_campo_fechamento),
                    erro = erroDe(CampoCartao.FECHAMENTO),
                    imeAction = ImeAction.Next,
                    modifier = Modifier.weight(1f),
                )
                CampoDia(
                    valor = estado.diaVencimento,
                    onValorChange = viewModel::alterarDiaVencimento,
                    rotulo = stringResource(R.string.cartao_campo_vencimento),
                    erro = erroDe(CampoCartao.VENCIMENTO),
                    imeAction = ImeAction.Done,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                stringResource(R.string.cartao_campo_dia_ajuda),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(stringResource(R.string.cartao_campo_fonte), style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                FONTES.forEachIndexed { indice, fonte ->
                    SegmentedButton(
                        selected = estado.fonte == fonte,
                        onClick = { viewModel.alterarFonte(fonte) },
                        shape = SegmentedButtonDefaults.itemShape(indice, FONTES.size),
                    ) { Text(stringResource(fonte.rotulo)) }
                }
            }
            erroDe(CampoCartao.FONTE)?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            Button(onClick = viewModel::salvar, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.acao_salvar))
            }
        }
    }

    estado.confirmarExclusao?.let { compras ->
        val recursos = LocalResources.current
        AlertDialog(
            onDismissRequest = viewModel::cancelarExclusao,
            title = { Text(stringResource(R.string.cartao_excluir_titulo)) },
            text = {
                Text(
                    if (compras == 0) {
                        stringResource(R.string.cartao_excluir_mensagem, estado.nome)
                    } else {
                        recursos.getQuantityString(R.plurals.cartao_excluir_com_compras, compras, estado.nome, compras)
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::excluir) { Text(stringResource(R.string.acao_excluir)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelarExclusao) { Text(stringResource(R.string.acao_cancelar)) }
            },
        )
    }
}

@Composable
private fun CampoDia(
    valor: String,
    onValorChange: (String) -> Unit,
    rotulo: String,
    erro: String?,
    imeAction: ImeAction,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onValorChange,
        label = { Text(rotulo) },
        singleLine = true,
        isError = erro != null,
        supportingText = erro?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = imeAction),
        modifier = modifier,
    )
}
