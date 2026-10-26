package br.com.meubolso.ui.conta

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.meubolso.R
import br.com.meubolso.util.Formatadores
import java.time.LocalDateTime
import java.time.ZoneId

/** Conta, orçamento compartilhado (código de convite e membros) e estado da sincronização. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContaScreen(onVoltar: () -> Unit, viewModel: ContaViewModel = hiltViewModel()) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.conta_titulo)) },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            estado.usuario?.let { usuario ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(usuario.nome, style = MaterialTheme.typography.titleMedium)
                        Text(usuario.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            estado.orcamento?.let { orcamento ->
                val convite = stringResource(R.string.conta_convite_mensagem, orcamento.nome, orcamento.codigoConvite)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(orcamento.nome, style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.conta_codigo_convite), style = MaterialTheme.typography.labelMedium)
                        Text(orcamento.codigoConvite, style = MaterialTheme.typography.headlineMedium, fontFamily = FontFamily.Monospace)
                        Text(stringResource(R.string.conta_codigo_ajuda), style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(onClick = {
                            val envio = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, convite)
                            context.startActivity(Intent.createChooser(envio, null))
                        }) { Text(stringResource(R.string.conta_compartilhar_codigo)) }
                        Text(
                            stringResource(R.string.conta_membros, orcamento.membros.values.sorted().joinToString()),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.conta_sincronizacao), style = MaterialTheme.typography.titleMedium)
                    val sinc = estado.sincronizacao
                    Text(
                        when {
                            sinc.sincronizando -> stringResource(R.string.conta_sincronizando)
                            sinc.falhou -> stringResource(R.string.conta_sincronizacao_falhou)
                            sinc.ultimaSincronizacao != null -> stringResource(
                                R.string.conta_ultima_sincronizacao,
                                Formatadores.dataHora(LocalDateTime.ofInstant(sinc.ultimaSincronizacao, ZoneId.systemDefault())),
                            )
                            else -> stringResource(R.string.conta_aguardando_sincronizacao)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (estado.pendentes > 0) {
                        Text(
                            pluralStringResource(R.plurals.conta_pendentes_envio, estado.pendentes, estado.pendentes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row {
                        TextButton(onClick = viewModel::sincronizarAgora, enabled = !sinc.sincronizando) {
                            Text(stringResource(R.string.conta_sincronizar_agora))
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = { viewModel.sair() },
                enabled = !estado.saindo,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.conta_sair)) }
            Text(
                stringResource(R.string.conta_sair_ajuda),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (estado.confirmarSaida) {
        AlertDialog(
            onDismissRequest = viewModel::cancelarSaida,
            title = { Text(stringResource(R.string.conta_sair_pendentes_titulo)) },
            text = { Text(stringResource(R.string.conta_sair_pendentes_texto)) },
            confirmButton = {
                TextButton(onClick = { viewModel.sair(forcar = true) }) { Text(stringResource(R.string.conta_sair_mesmo_assim)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelarSaida) { Text(stringResource(R.string.acao_cancelar)) }
            },
        )
    }
}
