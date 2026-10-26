package br.com.meubolso.ui.listacompras

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.meubolso.R
import br.com.meubolso.domain.model.ListaCompra
import br.com.meubolso.ui.components.SeletorMes
import br.com.meubolso.util.Formatadores

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListasCompraScreen(
    onNovaLista: () -> Unit,
    onAbrirLista: (Long) -> Unit,
    onEditarLista: (Long) -> Unit,
    viewModel: ListasCompraViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    var listaAExcluir by remember { mutableStateOf<ListaCompra?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val recursos = LocalResources.current

    LaunchedEffect(viewModel) {
        viewModel.duplicadas.collect { mes ->
            snackbar.showSnackbar(recursos.getString(R.string.listas_duplicada, Formatadores.mesAno(mes)))
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.listas_titulo)) }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNovaLista,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.listas_nova)) },
            )
        },
        bottomBar = { RodapeListas(estado) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(padding),
        ) {
            item {
                SeletorMes(mes = estado.mes, onMesChange = viewModel::alterarMes, modifier = Modifier.fillMaxWidth())
            }
            if (!estado.carregando && estado.listas.isEmpty()) {
                item { EstadoVazio() }
            }
            items(estado.listas, key = { it.lista.id }) { card ->
                CardLista(
                    card = card,
                    onAbrir = { onAbrirLista(card.lista.id) },
                    onEditar = { onEditarLista(card.lista.id) },
                    onExcluir = { listaAExcluir = card.lista },
                    onDuplicar = { viewModel.duplicar(card.lista) },
                )
            }
        }
    }

    listaAExcluir?.let { lista ->
        AlertDialog(
            onDismissRequest = { listaAExcluir = null },
            title = { Text(stringResource(R.string.listas_excluir_titulo)) },
            text = { Text(stringResource(R.string.listas_excluir_mensagem, lista.descricao)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.excluir(lista)
                    listaAExcluir = null
                }) { Text(stringResource(R.string.acao_excluir)) }
            },
            dismissButton = {
                TextButton(onClick = { listaAExcluir = null }) { Text(stringResource(R.string.acao_cancelar)) }
            },
        )
    }
}

@Composable
private fun CardLista(
    card: ListaCompraCardUi,
    onAbrir: () -> Unit,
    onEditar: () -> Unit,
    onExcluir: () -> Unit,
    onDuplicar: () -> Unit,
) {
    val lista = card.lista
    val resumo = card.resumo
    var menuAberto by rememberSaveable { mutableStateOf(false) }
    Card(onClick = onAbrir, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(lista.descricao, style = MaterialTheme.typography.titleMedium)
                    Text(
                        Formatadores.data(lista.data),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (lista.finalizada) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                    Text(
                        stringResource(R.string.listas_finalizada),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().padding(end = 8.dp)) {
                Contador(R.string.listas_total_itens, resumo.quantidadeItens, Modifier.weight(1f))
                Contador(R.string.listas_no_carrinho, resumo.itensNoCarrinho, Modifier.weight(1f))
                Contador(R.string.listas_desmarcados, resumo.itensDesmarcados, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(end = 8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.listas_preco_total), style = MaterialTheme.typography.labelMedium)
                    Text(
                        Formatadores.moeda(resumo.totalCentavos),
                        style = MaterialTheme.typography.titleLarge,
                        color = if (resumo.acimaDoLimite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    )
                }
                lista.limiteCentavos?.let {
                    Text(
                        stringResource(R.string.listas_limite, Formatadores.moeda(it)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            HorizontalDivider(Modifier.padding(top = 12.dp, end = 8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onAbrir) { Text(stringResource(R.string.listas_abrir)) }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onEditar) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.listas_editar))
                }
                IconButton(onClick = onExcluir) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.acao_excluir))
                }
                Box {
                    IconButton(onClick = { menuAberto = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null)
                    }
                    DropdownMenu(expanded = menuAberto, onDismissRequest = { menuAberto = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.listas_duplicar)) },
                            onClick = {
                                menuAberto = false
                                onDuplicar()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Contador(@StringRes rotulo: Int, valor: Int, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(stringResource(rotulo), style = MaterialTheme.typography.labelMedium)
        Text(valor.toString(), style = MaterialTheme.typography.titleMedium)
    }
}

/** Fixo no rodapé: quantidade de listas e soma do preço total delas no mês. */
@Composable
private fun RodapeListas(estado: ListasCompraUiState) {
    Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                pluralStringResource(R.plurals.listas_rodape_quantidade, estado.listas.size, estado.listas.size),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(stringResource(R.string.listas_rodape_total), style = MaterialTheme.typography.labelMedium)
                Text(Formatadores.moeda(estado.totalMesCentavos), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun EstadoVazio() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 48.dp),
    ) {
        Text(
            stringResource(R.string.listas_vazio_titulo),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.listas_vazio_descricao),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
