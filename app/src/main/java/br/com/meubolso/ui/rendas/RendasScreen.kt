package br.com.meubolso.ui.rendas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.meubolso.R
import br.com.meubolso.domain.model.ModoDiaRecebimento
import br.com.meubolso.domain.model.Renda
import br.com.meubolso.ui.components.LogoMeuBolso
import br.com.meubolso.ui.components.rotulo
import br.com.meubolso.util.Formatadores

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RendasScreen(
    onNovaRenda: () -> Unit,
    onEditarRenda: (Long) -> Unit,
    viewModel: RendasViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.rendas_titulo)) })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNovaRenda,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.rendas_nova)) },
            )
        },
    ) { padding ->
        when {
            estado.carregando -> Unit
            estado.itens.isEmpty() -> EstadoVazio(Modifier.padding(padding))
            else -> LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(padding),
            ) {
                item { CardTotal(estado.totalMensalAtivoCentavos) }
                items(estado.itens, key = { it.renda.id }) { item ->
                    CardRenda(item, onClick = { onEditarRenda(item.renda.id) })
                }
            }
        }
    }
}

@Composable
private fun CardTotal(totalCentavos: Long) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.rendas_total_mensal), style = MaterialTheme.typography.labelLarge)
            Text(Formatadores.moeda(totalCentavos), style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
private fun CardRenda(item: RendaItemUi, onClick: () -> Unit) {
    val renda = item.renda
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(16.dp)
                .alpha(if (renda.ativo) 1f else 0.6f),
        ) {
            Column(Modifier.weight(1f)) {
                Text(renda.descricao, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${stringResource(renda.tipo.rotulo)} · ${descricaoDia(renda)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = when {
                        !renda.ativo -> stringResource(R.string.rendas_inativa)
                        item.proximoRecebimento == null -> stringResource(R.string.rendas_sem_proximo)
                        else -> stringResource(
                            R.string.rendas_proximo_recebimento,
                            Formatadores.data(item.proximoRecebimento.prevista),
                        )
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                Formatadores.moeda(renda.valorCentavos),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun descricaoDia(renda: Renda): String = when (renda.modoDia) {
    ModoDiaRecebimento.DIA_FIXO -> stringResource(R.string.rendas_dia_fixo, renda.dia)
    ModoDiaRecebimento.DIA_UTIL -> stringResource(R.string.rendas_dia_util, renda.dia)
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
        LogoMeuBolso(altura = 40.dp, modifier = Modifier.padding(bottom = 24.dp))
        Text(
            stringResource(R.string.rendas_vazio_titulo),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.rendas_vazio_descricao),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
