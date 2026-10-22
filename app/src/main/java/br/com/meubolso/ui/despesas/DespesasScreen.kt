package br.com.meubolso.ui.despesas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.meubolso.R
import br.com.meubolso.domain.model.OcorrenciaDespesa
import br.com.meubolso.ui.components.PontoCor
import br.com.meubolso.ui.components.SeletorMes
import br.com.meubolso.ui.components.rotulo
import br.com.meubolso.util.Formatadores

private val COR_SEM_CATEGORIA = 0xFF7D8A80.toInt()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DespesasScreen(
    onNovaDespesa: () -> Unit,
    onEditarDespesa: (Long) -> Unit,
    viewModel: DespesasViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.despesas_titulo)) }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNovaDespesa,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.despesas_nova)) },
            )
        },
    ) { padding ->
        when {
            estado.carregando -> Unit
            !estado.possuiDespesas -> EstadoVazio(Modifier.padding(padding))
            else -> LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(padding),
            ) {
                item {
                    SeletorMes(
                        mes = estado.mes,
                        onMesChange = viewModel::alterarMes,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item { CardResumo(estado) }
                if (estado.ocorrencias.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.despesas_mes_vazio),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                        )
                    }
                }
                items(estado.ocorrencias, key = { "o${it.ocorrencia.despesa.id}" }) { item ->
                    CardOcorrencia(
                        item = item,
                        onClick = { onEditarDespesa(item.ocorrencia.despesa.id) },
                        onAlternarPagamento = { viewModel.alternarPagamento(item.ocorrencia) },
                    )
                }
                if (estado.semCobranca.isNotEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.despesas_fora_do_mes),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 16.dp, start = 4.dp),
                        )
                    }
                    items(estado.semCobranca, key = { "s${it.despesa.id}" }) { item ->
                        ItemSemCobranca(item, onClick = { onEditarDespesa(item.despesa.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun CardResumo(estado: DespesasUiState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.despesas_total_mes), style = MaterialTheme.typography.labelLarge)
            Text(Formatadores.moeda(estado.totalCentavos), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = {
                    if (estado.totalCentavos == 0L) 0f else estado.pagoCentavos.toFloat() / estado.totalCentavos
                },
                strokeCap = StrokeCap.Round,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                ValorRotulado(R.string.despesas_pago, estado.pagoCentavos, Modifier.weight(1f))
                ValorRotulado(R.string.despesas_pendente, estado.pendenteCentavos, Modifier.weight(1f), alinharFim = true)
            }
        }
    }
}

@Composable
private fun ValorRotulado(rotulo: Int, centavos: Long, modifier: Modifier = Modifier, alinharFim: Boolean = false) {
    Column(modifier, horizontalAlignment = if (alinharFim) Alignment.End else Alignment.Start) {
        Text(stringResource(rotulo), style = MaterialTheme.typography.labelMedium)
        Text(Formatadores.moeda(centavos), style = MaterialTheme.typography.titleMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardOcorrencia(item: OcorrenciaUi, onClick: () -> Unit, onAlternarPagamento: () -> Unit) {
    val ocorrencia = item.ocorrencia
    val despesa = ocorrencia.despesa
    val corErro = MaterialTheme.colorScheme.error
    Card(
        onClick = onClick,
        colors = if (item.vencida) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        } else {
            CardDefaults.cardColors()
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
            PontoCor(item.categoria?.cor ?: COR_SEM_CATEGORIA)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(despesa.descricao, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitulo(item),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    textoSituacao(ocorrencia, item.vencida),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (item.vencida) corErro else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    Formatadores.moeda(despesa.valorCentavos),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (item.vencida) corErro else MaterialTheme.colorScheme.onSurface,
                )
                FilterChip(
                    selected = ocorrencia.paga,
                    onClick = onAlternarPagamento,
                    label = {
                        Text(
                            stringResource(
                                if (ocorrencia.paga) R.string.despesas_status_paga else R.string.despesas_status_pendente,
                            ),
                        )
                    },
                    leadingIcon = if (ocorrencia.paga) {
                        { Icon(Icons.Default.Check, contentDescription = null, Modifier.height(FilterChipDefaults.IconSize)) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

@Composable
private fun subtitulo(item: OcorrenciaUi): String {
    val partes = buildList {
        item.categoria?.let { add(it.nome) }
        add(stringResource(item.ocorrencia.despesa.formaPagamento.rotulo))
        item.ocorrencia.parcela?.let { parcela ->
            add(stringResource(R.string.despesas_parcela, parcela, item.ocorrencia.despesa.totalParcelas ?: parcela))
        }
    }
    return partes.joinToString(" · ")
}

@Composable
private fun textoSituacao(ocorrencia: OcorrenciaDespesa, vencida: Boolean): String = when {
    ocorrencia.dataPagamento != null ->
        stringResource(R.string.despesas_paga_em, Formatadores.data(ocorrencia.dataPagamento))
    vencida -> stringResource(R.string.despesas_vencida_em, Formatadores.data(ocorrencia.vencimento))
    else -> stringResource(R.string.despesas_vence_em, Formatadores.data(ocorrencia.vencimento))
}

@Composable
private fun ItemSemCobranca(item: DespesaSemCobrancaUi, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        ListItem(
            leadingContent = { PontoCor(item.categoria?.cor ?: COR_SEM_CATEGORIA) },
            headlineContent = { Text(item.despesa.descricao) },
            supportingContent = {
                Text(
                    when (item.motivo) {
                        MotivoSemCobranca.INATIVA -> stringResource(R.string.despesas_motivo_inativa)
                        MotivoSemCobranca.ENCERRADA -> stringResource(R.string.despesas_motivo_encerrada)
                        MotivoSemCobranca.NAO_INICIADA -> stringResource(
                            R.string.despesas_motivo_inicia_em,
                            Formatadores.mesAno(item.despesa.mesInicio),
                        )
                    },
                )
            },
            trailingContent = { Text(Formatadores.moeda(item.despesa.valorCentavos)) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier.alpha(0.7f),
        )
    }
}

@Composable
private fun EstadoVazio(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
    ) {
        Text(
            stringResource(R.string.despesas_vazio_titulo),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.despesas_vazio_descricao),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
