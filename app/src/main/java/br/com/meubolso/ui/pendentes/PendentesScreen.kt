package br.com.meubolso.ui.pendentes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.meubolso.R
import br.com.meubolso.domain.model.NotificacaoBanco
import br.com.meubolso.domain.model.TipoTransacao
import br.com.meubolso.domain.notificacao.LeitorNotificacaoBanco
import br.com.meubolso.notificacao.AcessoNotificacoes
import br.com.meubolso.ui.components.rotulo
import br.com.meubolso.util.Formatadores
import java.time.LocalDateTime
import java.time.ZoneId

/** Transações lidas das notificações dos bancos, aguardando o usuário lançar ou descartar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendentesScreen(
    onVoltar: () -> Unit,
    onLancar: (Long) -> Unit,
    viewModel: PendentesViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // Rechecado ao voltar das configurações do sistema.
    var acessoConcedido by remember { mutableStateOf(AcessoNotificacoes.concedido(context)) }
    LifecycleResumeEffect(Unit) {
        acessoConcedido = AcessoNotificacoes.concedido(context)
        onPauseOrDispose { }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pendentes_titulo)) },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.acao_voltar))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(padding),
        ) {
            item {
                CardAcesso(
                    concedido = acessoConcedido,
                    onAtivar = { AcessoNotificacoes.abrirConfiguracao(context) },
                    onInformacoesApp = { AcessoNotificacoes.abrirInformacoesApp(context) },
                )
            }
            if (!estado.carregando && estado.pendentes.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.pendentes_vazio),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                    )
                }
            }
            items(estado.pendentes, key = { it.id }) { pendente ->
                CardPendente(
                    pendente = pendente,
                    onLancar = { onLancar(pendente.id) },
                    onDescartar = { viewModel.descartar(pendente.id) },
                )
            }
        }
    }
}

@Composable
private fun CardAcesso(concedido: Boolean, onAtivar: () -> Unit, onInformacoesApp: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (concedido) {
                MaterialTheme.colorScheme.surfaceContainerHigh
            } else {
                MaterialTheme.colorScheme.primaryContainer
            },
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(if (concedido) R.string.pendentes_acesso_ativo else R.string.pendentes_acesso_inativo),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                stringResource(
                    R.string.pendentes_bancos_monitorados,
                    LeitorNotificacaoBanco.BANCOS.joinToString { it.nome },
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (!concedido) {
                Text(stringResource(R.string.pendentes_acesso_ajuda), style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onAtivar) { Text(stringResource(R.string.pendentes_ativar)) }
                    TextButton(onClick = onInformacoesApp) { Text(stringResource(R.string.pendentes_informacoes_app)) }
                }
            }
        }
    }
}

@Composable
private fun CardPendente(pendente: NotificacaoBanco, onLancar: () -> Unit, onDescartar: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        pendente.descricaoSugerida ?: pendente.banco,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        "${pendente.banco} · ${stringResource(pendente.tipo.rotulo)} · " +
                            Formatadores.dataHora(LocalDateTime.ofInstant(pendente.recebidaEm, ZoneId.systemDefault())),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    pendente.valorCentavos?.let(Formatadores::moeda) ?: stringResource(R.string.pendentes_sem_valor),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            // Texto original, para conferir o que foi lido.
            Text(
                listOf(pendente.titulo, pendente.texto).filter(String::isNotBlank).joinToString("\n"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (pendente.tipo == TipoTransacao.ENTRADA) {
                Text(
                    stringResource(R.string.pendentes_aviso_entrada),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                OutlinedButton(onClick = onDescartar) { Text(stringResource(R.string.pendentes_descartar)) }
                Button(onClick = onLancar) { Text(stringResource(R.string.pendentes_lancar)) }
            }
        }
    }
}
