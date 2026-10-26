package br.com.meubolso.ui.conta

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.meubolso.R
import br.com.meubolso.data.remoto.Usuario
import br.com.meubolso.ui.components.LogoMeuBolso

/** Depois do login: criar um orçamento (e convidar alguém) ou entrar no de outra pessoa com o código. */
@Composable
fun OrcamentoScreen(usuario: Usuario, viewModel: OrcamentoViewModel = hiltViewModel()) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    Surface(Modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            LogoMeuBolso(altura = 40.dp, modifier = Modifier.padding(top = 24.dp))
            Text(stringResource(R.string.orcamento_ola, usuario.nome), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.orcamento_explicacao),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.orcamento_criar_titulo), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.orcamento_criar_descricao), style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = estado.nome,
                        onValueChange = viewModel::alterarNome,
                        label = { Text(stringResource(R.string.orcamento_nome)) },
                        placeholder = { Text(stringResource(R.string.orcamento_nome_exemplo)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(onClick = viewModel::criar, enabled = !estado.carregando, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.orcamento_criar))
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.orcamento_entrar_titulo), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.orcamento_entrar_descricao), style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = estado.codigo,
                        onValueChange = viewModel::alterarCodigo,
                        label = { Text(stringResource(R.string.orcamento_codigo)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedButton(onClick = viewModel::entrar, enabled = !estado.carregando, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.orcamento_entrar))
                    }
                }
            }

            estado.erro?.let {
                Text(stringResource(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            TextButton(onClick = viewModel::sair, enabled = !estado.carregando) {
                Text(stringResource(R.string.orcamento_outra_conta, usuario.email))
            }
        }
    }
}
