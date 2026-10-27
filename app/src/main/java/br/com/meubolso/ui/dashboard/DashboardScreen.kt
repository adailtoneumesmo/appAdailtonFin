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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.meubolso.R
import br.com.meubolso.domain.dashboard.Balanco
import br.com.meubolso.domain.dashboard.ConsumoCategoria
import br.com.meubolso.domain.dashboard.FaixaConsumo
import br.com.meubolso.domain.dashboard.SituacaoSaldo
import br.com.meubolso.domain.objetivo.ResumoObjetivos
import br.com.meubolso.ui.components.LogoMeuBolso
import br.com.meubolso.ui.components.SeletorMes
import br.com.meubolso.ui.objetivos.ResumoTotalObjetivos
import br.com.meubolso.ui.objetivos.VazioObjetivos
import br.com.meubolso.ui.theme.CoresMarca
import br.com.meubolso.ui.theme.LocalTemaEscuro
import br.com.meubolso.ui.theme.MeuBolsoTheme
import br.com.meubolso.util.Formatadores
import java.time.YearMonth
import kotlin.math.roundToInt

/** Início: só o resumo geral do mês; salário e vale ficam no detalhamento. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNovaRenda: () -> Unit,
    onNovaDespesa: () -> Unit,
    onDetalhar: (YearMonth) -> Unit,
    onConfiguracoes: () -> Unit,
    onPendentes: () -> Unit,
    onObjetivos: () -> Unit,
    onNovoObjetivo: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val resumo = estado.resumo
    val pendentes by viewModel.quantidadePendentes.collectAsStateWithLifecycle()
    val objetivos by viewModel.resumoObjetivos.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { LogoMeuBolso() },
                actions = {
                    IconButton(onClick = onConfiguracoes) {
                        Icon(painterResource(R.drawable.ic_aba_configuracoes), contentDescription = stringResource(R.string.configuracoes_titulo))
                    }
                },
            )
        },
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
            if (pendentes > 0) {
                item { CardPendentes(pendentes, onPendentes) }
            }
            when {
                estado.carregando -> Unit
                !resumo.possuiLancamentos -> item { EstadoVazioMes(onNovaRenda, onNovaDespesa) }
                else -> item {
                    CardResumoGeral(
                        geral = resumo.geral,
                        consumo = estado.consumoPorCategoria,
                        onDetalhar = { onDetalhar(estado.mes) },
                    )
                }
            }
            objetivos?.let { resumo ->
                item { CardObjetivos(resumo, onObjetivos, onNovoObjetivo) }
            }
        }
    }
}

@Composable
private fun CardPendentes(quantidade: Int, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
            Column(Modifier.weight(1f)) {
                Text(
                    pluralStringResource(R.plurals.dashboard_pendentes, quantidade, quantidade),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(stringResource(R.string.dashboard_pendentes_descricao), style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Composable
private fun CardObjetivos(resumo: ResumoObjetivos, onDetalhar: () -> Unit, onNovoObjetivo: () -> Unit) {
    Card(shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text(stringResource(R.string.objetivos_titulo), style = MaterialTheme.typography.titleMedium)
            if (resumo.itens.isEmpty()) {
                VazioObjetivos(onNovoObjetivo)
                return@Card
            }
            if (resumo.posicao == null) {
                Text(
                    stringResource(R.string.objetivos_patrimonio_nao_informado),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            ResumoTotalObjetivos(resumo, Modifier.padding(top = 12.dp))
            Spacer(Modifier.height(16.dp))
            Button(onClick = onDetalhar, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.objetivos_detalhar))
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, Modifier.size(ButtonDefaults.IconSize))
            }
        }
    }
}

@Composable
private fun CardResumoGeral(geral: Balanco, consumo: List<ConsumoCategoria>, onDetalhar: () -> Unit) {
    // Card de destaque nas cores da marca (tinta + limão) nos dois temas.
    val temaClaro = !LocalTemaEscuro.current
    MeuBolsoTheme(escuro = true) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (temaClaro) CoresMarca.Tinta else MaterialTheme.colorScheme.surfaceContainerHighest,
            ),
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth(),
        ) {
            ConteudoResumoGeral(geral, consumo, onDetalhar)
        }
    }
}

@Composable
private fun ConteudoResumoGeral(geral: Balanco, consumo: List<ConsumoCategoria>, onDetalhar: () -> Unit) {
    Column(Modifier.padding(20.dp)) {
        Text(stringResource(R.string.dashboard_resumo_geral), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        IndicadorSaldo(R.string.dashboard_saldo_geral, geral, destaque = true)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth()) {
            ValorRotulado(R.string.dashboard_total_receitas, geral.rendaCentavos, Modifier.weight(1f))
            ValorRotulado(R.string.dashboard_total_despesas, geral.despesaCentavos, Modifier.weight(1f), alinharFim = true)
        }
        Spacer(Modifier.height(12.dp))
        BarraConsumo(geral)

        HorizontalDivider(Modifier.padding(vertical = 16.dp))
        Text(stringResource(R.string.dashboard_consumo_categoria), style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(12.dp))
        if (consumo.isEmpty()) {
            Text(
                stringResource(R.string.dashboard_sem_despesas),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            val semCategoria = stringResource(R.string.dashboard_sem_categoria)
            val corSemCategoria = MaterialTheme.colorScheme.outline
            GraficoConsumo(
                fatias = consumo.map {
                    FatiaGrafico(
                        rotulo = it.categoria?.nome ?: semCategoria,
                        cor = it.categoria?.let { c -> Color(c.cor) } ?: corSemCategoria,
                        centavos = it.centavos,
                    )
                },
            )
        }

        Spacer(Modifier.height(16.dp))
        Button(onClick = onDetalhar, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.dashboard_detalhar))
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, Modifier.size(ButtonDefaults.IconSize))
        }
    }
}

/** Quanto da renda já foi para despesas, com a cor da [FaixaConsumo]. */
@Composable
internal fun BarraConsumo(balanco: Balanco) {
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
}

/** Saldo com cor, seta e texto: verde/positivo, vermelho/negativo, cinza/zerado. */
@Composable
internal fun IndicadorSaldo(@StringRes rotulo: Int, balanco: Balanco, destaque: Boolean = false) {
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
                style = if (destaque) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineSmall,
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
    FaixaConsumo.ALERTA -> CoresMarca.Alerta
    FaixaConsumo.EXCEDIDO -> MaterialTheme.colorScheme.error
}

@Composable
private fun textoConsumo(balanco: Balanco): String = when {
    balanco.fracaoConsumida.isInfinite() -> stringResource(R.string.dashboard_consumo_sem_renda)
    else -> stringResource(R.string.dashboard_consumo, (balanco.fracaoConsumida * 100).roundToInt())
}

@Composable
internal fun ValorRotulado(@StringRes rotulo: Int, centavos: Long, modifier: Modifier = Modifier, alinharFim: Boolean = false) {
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
