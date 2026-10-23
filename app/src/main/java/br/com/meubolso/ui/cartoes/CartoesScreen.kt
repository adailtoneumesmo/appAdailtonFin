package br.com.meubolso.ui.cartoes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.meubolso.R
import br.com.meubolso.domain.model.Fatura
import br.com.meubolso.ui.components.rotulo
import br.com.meubolso.util.Formatadores

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartoesScreen(
    onNovoCartao: () -> Unit,
    onEditarCartao: (Long) -> Unit,
    onAbrirFatura: (Long) -> Unit,
    viewModel: CartoesViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.cartoes_titulo)) }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNovoCartao,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.cartoes_novo)) },
            )
        },
    ) { padding ->
        when {
            estado.carregando -> Unit
            estado.faturasAtuais.isEmpty() -> EstadoVazio(Modifier.padding(padding))
            else -> LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(padding),
            ) {
                items(estado.faturasAtuais, key = { it.cartao.id }) { fatura ->
                    CardCartao(
                        fatura = fatura,
                        onClick = { onAbrirFatura(fatura.cartao.id) },
                        onEditar = { onEditarCartao(fatura.cartao.id) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardCartao(fatura: Fatura, onClick: () -> Unit, onEditar: () -> Unit) {
    val cartao = fatura.cartao
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(cartao.nome, style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.cartoes_fecha_vence, cartao.diaFechamento, cartao.diaVencimento) +
                        " · " + stringResource(cartao.fonte.rotulo),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(R.string.cartoes_fatura_atual, Formatadores.mesAno(fatura.mes)),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(Formatadores.moeda(fatura.totalCentavos), style = MaterialTheme.typography.titleLarge)
            }
            IconButton(onClick = onEditar) {
                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.cartoes_editar))
            }
        }
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
            stringResource(R.string.cartoes_vazio_titulo),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.cartoes_vazio_descricao),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
