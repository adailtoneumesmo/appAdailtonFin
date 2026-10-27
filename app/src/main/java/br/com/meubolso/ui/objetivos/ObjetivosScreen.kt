package br.com.meubolso.ui.objetivos

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.meubolso.R
import br.com.meubolso.domain.model.PosicaoInvestimentos
import br.com.meubolso.domain.objetivo.ProgressoObjetivo
import br.com.meubolso.domain.objetivo.ResumoObjetivos
import br.com.meubolso.ui.components.CampoMoeda
import br.com.meubolso.util.Formatadores

/** Detalhamento: patrimônio investido, a soma de todos os objetivos e o progresso de cada um. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObjetivosScreen(
    onVoltar: () -> Unit,
    onNovoObjetivo: () -> Unit,
    onEditarObjetivo: (Long) -> Unit,
    viewModel: ObjetivosViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val edicaoPatrimonio by viewModel.edicaoPatrimonio.collectAsStateWithLifecycle()
    val resumo = estado.resumo

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.objetivos_titulo)) },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.acao_voltar))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNovoObjetivo,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.objetivos_novo)) },
            )
        },
    ) { padding ->
        if (estado.carregando) return@Scaffold
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(padding),
        ) {
            item { CardPatrimonio(resumo.posicao, viewModel::abrirEdicaoPatrimonio) }
            if (resumo.itens.isEmpty()) {
                item { VazioObjetivos(onNovoObjetivo) }
            } else {
                item { CardTotal(resumo) }
                items(resumo.itens, key = { it.objetivo.id }) { item ->
                    CardObjetivo(item, onEditar = { onEditarObjetivo(item.objetivo.id) })
                }
                item {
                    Text(
                        stringResource(R.string.objetivos_prioridade_ajuda),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    edicaoPatrimonio?.let { valor ->
        AlertDialog(
            onDismissRequest = viewModel::cancelarEdicaoPatrimonio,
            title = { Text(stringResource(R.string.objetivos_patrimonio)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.objetivos_patrimonio_ajuda), style = MaterialTheme.typography.bodyMedium)
                    CampoMoeda(
                        valorCentavos = valor,
                        onValorChange = viewModel::alterarPatrimonio,
                        rotulo = stringResource(R.string.objetivos_patrimonio),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::salvarPatrimonio) { Text(stringResource(R.string.acao_salvar)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelarEdicaoPatrimonio) { Text(stringResource(R.string.acao_cancelar)) }
            },
        )
    }
}

@Composable
private fun CardPatrimonio(posicao: PosicaoInvestimentos?, onAtualizar: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.objetivos_patrimonio), style = MaterialTheme.typography.labelMedium)
            if (posicao == null) {
                Text(
                    stringResource(R.string.objetivos_patrimonio_nao_informado),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            } else {
                Text(Formatadores.moeda(posicao.valorCentavos), style = MaterialTheme.typography.headlineSmall)
                Text(
                    stringResource(R.string.objetivos_patrimonio_atualizado, Formatadores.data(posicao.dataReferencia)),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Button(onClick = onAtualizar, modifier = Modifier.padding(top = 12.dp)) {
                Text(
                    stringResource(
                        if (posicao == null) R.string.objetivos_patrimonio_informar else R.string.objetivos_patrimonio_atualizar,
                    ),
                )
            }
        }
    }
}

@Composable
private fun CardTotal(resumo: ResumoObjetivos) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.objetivos_meta_total), style = MaterialTheme.typography.titleMedium)
            ResumoTotalObjetivos(resumo, Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun CardObjetivo(item: ProgressoObjetivo, onEditar: () -> Unit) {
    val objetivo = item.objetivo
    Card(onClick = onEditar, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 16.dp, bottom = 16.dp, end = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(objetivo.nome, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = onEditar) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.objetivos_editar))
                }
            }
            Text(
                stringResource(
                    R.string.objetivos_progresso,
                    Formatadores.moeda(item.realizadoCentavos),
                    Formatadores.moeda(objetivo.valorAlvoCentavos),
                ),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            BarraObjetivo(item)
            objetivo.dataAlvo?.let { data ->
                val prazo = when {
                    item.prazoVencido -> stringResource(R.string.objetivos_prazo_vencido)
                    item.porMesCentavos != null -> stringResource(R.string.objetivos_por_mes, Formatadores.moeda(item.porMesCentavos))
                    else -> null
                }
                Text(
                    listOfNotNull(stringResource(R.string.objetivos_ate, Formatadores.data(data)), prazo).joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (item.prazoVencido) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
internal fun VazioObjetivos(onCriar: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.padding(vertical = 8.dp)) {
        Text(stringResource(R.string.objetivos_vazio_titulo), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.objetivos_vazio_descricao),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )
        OutlinedButton(onClick = onCriar) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text(stringResource(R.string.objetivos_criar), modifier = Modifier.padding(start = 8.dp))
        }
    }
}
