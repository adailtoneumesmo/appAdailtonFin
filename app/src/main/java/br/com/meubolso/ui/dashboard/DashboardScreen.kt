package br.com.meubolso.ui.dashboard

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.meubolso.R
import br.com.meubolso.domain.dashboard.Balanco
import br.com.meubolso.domain.dashboard.FaixaConsumo
import br.com.meubolso.domain.dashboard.ResumoDashboard
import br.com.meubolso.domain.dashboard.SituacaoSaldo
import br.com.meubolso.ui.components.LogoMeuBolso
import br.com.meubolso.ui.components.SeletorMes
import br.com.meubolso.ui.theme.CoresMarca
import br.com.meubolso.util.Formatadores
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNovaRenda: () -> Unit,
    onNovaDespesa: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val resumo = estado.resumo

    Scaffold(
        topBar = { TopAppBar(title = { LogoMeuBolso() }) },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(padding),
        ) {
            item {
                SeletorMes(
                    mes = estado.mes,
                    onMesChange = viewModel::alterarMes,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            when {
                estado.carregando -> Unit
                !resumo.possuiLancamentos -> item { EstadoVazioMes(onNovaRenda, onNovaDespesa) }
                else -> {
                    item { CardResumoGeral(resumo.geral) }
                    item {
                        CardOrigem(
                            titulo = R.string.dashboard_salario,
                            balanco = resumo.salario,
                            observacao = resumo.rendaExtraCentavos.takeIf { it > 0 }?.let {
                                stringResource(R.string.dashboard_inclui_renda_extra, Formatadores.moeda(it))
                            },
                        )
                    }
                    item { CardOrigem(titulo = R.string.dashboard_vale, balanco = resumo.vale) }
                    item { CardGrafico(resumo) }
                }
            }
        }
    }
}

@Composable
private fun CardResumoGeral(geral: Balanco) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.dashboard_resumo_geral), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                ValorRotulado(R.string.dashboard_total_receitas, geral.rendaCentavos, Modifier.weight(1f))
                ValorRotulado(R.string.dashboard_total_despesas, geral.despesaCentavos, Modifier.weight(1f), alinharFim = true)
            }
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            IndicadorSaldo(R.string.dashboard_saldo_geral, geral)
        }
    }
}

@Composable
private fun CardOrigem(@StringRes titulo: Int, balanco: Balanco, observacao: String? = null) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(titulo), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                ValorRotulado(R.string.dashboard_renda, balanco.rendaCentavos, Modifier.weight(1f))
                ValorRotulado(R.string.dashboard_despesas, balanco.despesaCentavos, Modifier.weight(1f), alinharFim = true)
            }
            observacao?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { balanco.fracaoConsumida.coerceAtMost(1.0).toFloat() },
                color = corDaFaixa(balanco.faixaConsumo),
                // Trilho neutro: o padrão (amarelado) se confundiria com a faixa de alerta.
                trackColor = MaterialTheme.colorScheme.outlineVariant,
                strokeCap = StrokeCap.Round,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
            )
            Text(
                textoConsumo(balanco),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            IndicadorSaldo(R.string.dashboard_saldo, balanco)
        }
    }
}

@Composable
private fun CardGrafico(resumo: ResumoDashboard) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.dashboard_grafico_titulo), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            GraficoComparativo(
                grupos = listOf(
                    GrupoGrafico(
                        stringResource(R.string.dashboard_salario),
                        resumo.salario.rendaCentavos,
                        resumo.salario.despesaCentavos,
                    ),
                    GrupoGrafico(
                        stringResource(R.string.dashboard_vale),
                        resumo.vale.rendaCentavos,
                        resumo.vale.despesaCentavos,
                    ),
                ),
            )
        }
    }
}

/** Saldo com cor, seta e texto: verde/positivo, vermelho/negativo, cinza/zerado. */
@Composable
private fun IndicadorSaldo(@StringRes rotulo: Int, balanco: Balanco) {
    val (cor, icone, texto) = when (balanco.situacao) {
        SituacaoSaldo.POSITIVO -> Triple(MaterialTheme.colorScheme.primary, R.drawable.ic_seta_cima, R.string.dashboard_saldo_positivo)
        SituacaoSaldo.NEGATIVO -> Triple(MaterialTheme.colorScheme.error, R.drawable.ic_seta_baixo, R.string.dashboard_saldo_negativo)
        SituacaoSaldo.ZERADO -> Triple(MaterialTheme.colorScheme.onSurfaceVariant, R.drawable.ic_saldo_zerado, R.string.dashboard_saldo_zerado)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(rotulo), style = MaterialTheme.typography.labelMedium)
            Text(
                Formatadores.moeda(balanco.saldoCentavos),
                style = MaterialTheme.typography.headlineSmall,
                color = cor,
            )
        }
        Icon(painterResource(icone), contentDescription = null, tint = cor, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(4.dp))
        Text(stringResource(texto), style = MaterialTheme.typography.labelLarge, color = cor)
    }
}

@Composable
private fun corDaFaixa(faixa: FaixaConsumo): Color = when (faixa) {
    FaixaConsumo.NORMAL -> MaterialTheme.colorScheme.primary
    FaixaConsumo.ALERTA -> CoresMarca.Amarelo
    FaixaConsumo.EXCEDIDO -> MaterialTheme.colorScheme.error
}

@Composable
private fun textoConsumo(balanco: Balanco): String = when {
    balanco.fracaoConsumida.isInfinite() -> stringResource(R.string.dashboard_consumo_sem_renda)
    else -> stringResource(R.string.dashboard_consumo, (balanco.fracaoConsumida * 100).roundToInt())
}

@Composable
private fun ValorRotulado(@StringRes rotulo: Int, centavos: Long, modifier: Modifier = Modifier, alinharFim: Boolean = false) {
    Column(modifier, horizontalAlignment = if (alinharFim) Alignment.End else Alignment.Start) {
        Text(stringResource(rotulo), style = MaterialTheme.typography.labelMedium)
        Text(Formatadores.moeda(centavos), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun EstadoVazioMes(onNovaRenda: () -> Unit, onNovaDespesa: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 48.dp),
    ) {
        Text(
            stringResource(R.string.dashboard_vazio_titulo),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.dashboard_vazio_descricao),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onNovaRenda) {
                Icon(Icons.Default.Add, contentDescription = null, Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.dashboard_adicionar_renda))
            }
            OutlinedButton(onClick = onNovaDespesa) {
                Icon(Icons.Default.Add, contentDescription = null, Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.dashboard_adicionar_despesa))
            }
        }
    }
}
