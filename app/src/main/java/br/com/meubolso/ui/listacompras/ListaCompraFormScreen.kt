package br.com.meubolso.ui.listacompras

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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import br.com.meubolso.domain.listacompra.CampoListaCompra
import br.com.meubolso.ui.components.CampoData
import br.com.meubolso.ui.components.CampoMoeda
import br.com.meubolso.ui.components.mensagem
import br.com.meubolso.util.Formatadores

/** [onSalva] recebe o id da lista e se ela acabou de ser criada. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaCompraFormScreen(
    onVoltar: () -> Unit,
    onSalva: (id: Long, nova: Boolean) -> Unit,
    viewModel: ListaCompraFormViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val sugestao by viewModel.sugestaoVale.collectAsStateWithLifecycle()

    LaunchedEffect(estado.salvaId) {
        estado.salvaId?.let { onSalva(it, !estado.editando) }
    }

    @Composable
    fun erroDe(campo: CampoListaCompra): String? = estado.erros[campo]?.let { stringResource(it.mensagem) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (estado.editando) R.string.lista_form_titulo_editar else R.string.lista_form_titulo_nova,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.acao_voltar))
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
                label = { Text(stringResource(R.string.lista_campo_descricao)) },
                placeholder = { Text(stringResource(R.string.lista_campo_descricao_dica)) },
                singleLine = true,
                isError = estado.erros.containsKey(CampoListaCompra.DESCRICAO),
                supportingText = erroDe(CampoListaCompra.DESCRICAO)?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            CampoData(
                rotulo = stringResource(R.string.lista_campo_data),
                data = estado.data,
                onDataChange = viewModel::alterarData,
                erro = erroDe(CampoListaCompra.DATA),
                modifier = Modifier.fillMaxWidth(),
            )

            CampoMoeda(
                valorCentavos = estado.limiteCentavos,
                onValorChange = viewModel::alterarLimite,
                rotulo = stringResource(R.string.lista_campo_limite),
                modifier = Modifier.fillMaxWidth(),
            )

            sugestao?.let { s ->
                val mes = Formatadores.mesAnoCompacto(s.mes)
                OutlinedButton(
                    onClick = viewModel::usarSaldoVale,
                    enabled = s.possuiCredito,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.lista_usar_vale)) }
                TextoAjuda(
                    when {
                        !s.possuiCredito -> stringResource(R.string.lista_vale_sem_credito, mes)
                        s.saldo.gastoCentavos == 0L -> stringResource(
                            R.string.lista_vale_projetado,
                            mes,
                            Formatadores.moeda(s.saldo.creditoCentavos),
                        )
                        else -> stringResource(
                            R.string.lista_vale_disponivel,
                            mes,
                            Formatadores.moeda(s.saldo.saldoCentavos),
                            Formatadores.moeda(s.saldo.creditoCentavos),
                            Formatadores.moeda(s.saldo.gastoCentavos),
                        )
                    },
                )
            }
            estado.limiteDoVale?.let {
                Text(
                    stringResource(R.string.lista_limite_do_vale, Formatadores.mesAnoCompacto(it)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Button(onClick = viewModel::salvar, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.acao_salvar))
            }
        }
    }
}

@Composable
private fun TextoAjuda(texto: String) {
    Text(texto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
