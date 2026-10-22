package br.com.meubolso.ui.categorias

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.meubolso.R
import br.com.meubolso.domain.model.Categoria
import br.com.meubolso.ui.components.PontoCor
import br.com.meubolso.ui.components.mensagem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriasScreen(
    onVoltar: () -> Unit,
    viewModel: CategoriasViewModel = hiltViewModel(),
) {
    val categorias by viewModel.categorias.collectAsStateWithLifecycle()
    val dialogo by viewModel.dialogo.collectAsStateWithLifecycle()
    val exclusaoBloqueada by viewModel.exclusaoBloqueada.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val recursos = LocalResources.current

    LaunchedEffect(exclusaoBloqueada) {
        exclusaoBloqueada?.let { qtd ->
            snackbar.showSnackbar(recursos.getQuantityString(R.plurals.categorias_em_uso, qtd, qtd))
            viewModel.avisoExibido()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.categorias_titulo)) },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.acao_voltar))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = viewModel::novaCategoria,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.categorias_nova)) },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(bottom = 96.dp),
            modifier = Modifier.padding(padding),
        ) {
            items(categorias, key = { it.id }) { categoria ->
                ItemCategoria(
                    categoria = categoria,
                    onEditar = { viewModel.editar(categoria) },
                    onExcluir = { viewModel.excluir(categoria) },
                )
            }
        }
    }

    dialogo?.let { atual ->
        DialogoCategoriaForm(
            dialogo = atual,
            onNomeChange = viewModel::alterarNome,
            onCorChange = viewModel::alterarCor,
            onSalvar = viewModel::salvar,
            onCancelar = viewModel::fecharDialogo,
        )
    }
}

@Composable
private fun ItemCategoria(categoria: Categoria, onEditar: () -> Unit, onExcluir: () -> Unit) {
    ListItem(
        leadingContent = { PontoCor(categoria.cor, tamanho = 20.dp) },
        headlineContent = { Text(categoria.nome) },
        supportingContent = {
            Text(stringResource(if (categoria.padrao) R.string.categorias_padrao else R.string.categorias_personalizada))
        },
        trailingContent = if (categoria.padrao) {
            null
        } else {
            {
                IconButton(onClick = onExcluir) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.acao_excluir))
                }
            }
        },
        modifier = if (categoria.padrao) Modifier else Modifier.clickable(onClick = onEditar),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DialogoCategoriaForm(
    dialogo: DialogoCategoria,
    onNomeChange: (String) -> Unit,
    onCorChange: (Int) -> Unit,
    onSalvar: () -> Unit,
    onCancelar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = {
            Text(stringResource(if (dialogo.id == null) R.string.categorias_nova else R.string.categorias_editar))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = dialogo.nome,
                    onValueChange = onNomeChange,
                    label = { Text(stringResource(R.string.categorias_campo_nome)) },
                    singleLine = true,
                    isError = dialogo.erro != null,
                    supportingText = dialogo.erro?.let { { Text(stringResource(it.mensagem)) } },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(stringResource(R.string.categorias_campo_cor), style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PALETA_CATEGORIAS.forEach { cor ->
                        val selecionada = cor == dialogo.cor
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .border(
                                    width = if (selecionada) 3.dp else 0.dp,
                                    color = if (selecionada) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape,
                                )
                                .padding(4.dp)
                                .clickable { onCorChange(cor) },
                        ) {
                            PontoCor(cor, tamanho = 32.dp)
                            if (selecionada) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = stringResource(R.string.categorias_cor_selecionada),
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onSalvar) { Text(stringResource(R.string.acao_salvar)) } },
        dismissButton = { TextButton(onClick = onCancelar) { Text(stringResource(R.string.acao_cancelar)) } },
    )
}
