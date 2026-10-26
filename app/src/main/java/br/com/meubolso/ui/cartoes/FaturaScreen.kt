package br.com.meubolso.ui.cartoes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.meubolso.R
import br.com.meubolso.domain.model.Fatura
import br.com.meubolso.domain.model.LancamentoFatura
import br.com.meubolso.domain.model.ModalidadeCompra
import br.com.meubolso.ui.components.PontoCor
import br.com.meubolso.ui.components.SeletorMes
import br.com.meubolso.ui.components.rotulo
import br.com.meubolso.util.Formatadores

private val COR_SEM_CATEGORIA = 0xFF7D8A80.toInt()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaturaScreen(
    onVoltar: () -> Unit,
    onEditarCartao: (Long) -> Unit,
    onNovaDespesa: (Long) -> Unit,
    onEditarDespesa: (Long) -> Unit,
    viewModel: FaturaViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val fatura = estado.fatura

    // Cartão excluído enquanto a fatura estava aberta na pilha.
    LaunchedEffect(estado.carregando, fatura) {
        if (!estado.carregando && fatura == null) onVoltar()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(fatura?.cartao?.nome.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.acao_voltar))
                    }
                },
                actions = {
                    if (fatura != null) {
                        IconButton(onClick = { onEditarCartao(fatura.cartao.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.cartoes_editar))
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (fatura != null) {
                ExtendedFloatingActionButton(
                    onClick = { onNovaDespesa(fatura.cartao.id) },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.fatura_nova_despesa)) },
                )
            }
        },
    ) { padding ->
        if (fatura == null) return@Scaffold
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(padding),
        ) {
            item {
                SeletorMes(mes = fatura.mes, onMesChange = viewModel::alterarMes, modifier = Modifier.fillMaxWidth())
            }
            item { CardResumo(fatura, estado.fechada) }
            if (estado.lancamentos.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.fatura_vazia),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                    )
                }
            }
            items(estado.lancamentos, key = { it.lancamento.compra.id }) { item ->
                CardLancamento(item, onClick = { onEditarDespesa(item.lancamento.compra.id) })
            }
        }
    }
}

@Composable
private fun CardResumo(fatura: Fatura, fechada: Boolean) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.fatura_total),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f),
                )
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(stringResource(if (fechada) R.string.fatura_fechada else R.string.fatura_aberta)) },
                )
            }
            Text(Formatadores.moeda(fatura.totalCentavos), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                ValorRotulado(R.string.fatura_fechamento, Formatadores.data(fatura.fechamento), Modifier.weight(1f))
                ValorRotulado(R.string.fatura_vencimento, Formatadores.data(fatura.vencimento), Modifier.weight(1f))
                ValorRotulado(
                    R.string.fatura_fonte,
                    stringResource(fatura.cartao.fonte.rotulo),
                    Modifier.weight(1f),
                    alinharFim = true,
                )
            }
        }
    }
}

@Composable
private fun ValorRotulado(rotulo: Int, valor: String, modifier: Modifier = Modifier, alinharFim: Boolean = false) {
    Column(modifier, horizontalAlignment = if (alinharFim) Alignment.End else Alignment.Start) {
        Text(stringResource(rotulo), style = MaterialTheme.typography.labelMedium)
        Text(valor, style = MaterialTheme.typography.titleSmall)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardLancamento(item: LancamentoUi, onClick: () -> Unit) {
    val lancamento = item.lancamento
    val compra = lancamento.compra
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
            PontoCor(item.categoria?.cor ?: COR_SEM_CATEGORIA)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(compra.descricao, style = MaterialTheme.typography.titleMedium)
                Text(
                    listOfNotNull(item.categoria?.nome, Formatadores.data(compra.dataCompra)).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                indicacao(lancamento)?.let {
                    Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                Formatadores.moeda(lancamento.valorCentavos),
                style = MaterialTheme.typography.titleMedium,
                color = if (compra.estorno) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** "Parcela 2/5 · faltam 3", "Parcela 5/5 · última", "Fixo" ou nada para compra à vista; estorno vem na frente. */
@Composable
private fun indicacao(lancamento: LancamentoFatura): String? {
    val estorno = if (lancamento.compra.estorno) stringResource(R.string.fatura_estorno) else null
    return listOfNotNull(estorno, indicacaoModalidade(lancamento)).joinToString(" · ").ifEmpty { null }
}

@Composable
private fun indicacaoModalidade(lancamento: LancamentoFatura): String? {
    val recursos = LocalResources.current
    return when (lancamento.compra.modalidade) {
        ModalidadeCompra.A_VISTA -> null
        ModalidadeCompra.FIXO -> stringResource(R.string.fatura_fixo)
        ModalidadeCompra.PARCELADO -> {
            val parcela = checkNotNull(lancamento.parcela)
            val restantes = checkNotNull(lancamento.parcelasRestantes)
            val total = parcela + restantes
            val complemento = if (restantes == 0) {
                stringResource(R.string.fatura_ultima_parcela)
            } else {
                recursos.getQuantityString(R.plurals.fatura_parcelas_restantes, restantes, restantes)
            }
            stringResource(R.string.fatura_parcela, parcela, total) + " · " + complemento
        }
    }
}
