package br.com.meubolso.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import br.com.meubolso.R
import br.com.meubolso.util.Formatadores

data class GrupoGrafico(val rotulo: String, val rendaCentavos: Long, val despesaCentavos: Long)

/** Renda e despesa, nesta ordem, dentro de cada grupo. */
private const val SERIES = 2

/**
 * Barras agrupadas desenhadas à mão (duas barras por grupo dispensam biblioteca).
 * Tocar numa barra mostra o valor dela; tocar de novo esconde.
 */
@Composable
fun GraficoComparativo(grupos: List<GrupoGrafico>, modifier: Modifier = Modifier) {
    val corRenda = MaterialTheme.colorScheme.primary
    val corDespesa = MaterialTheme.colorScheme.error
    val corEixo = MaterialTheme.colorScheme.outlineVariant
    val corRotulo = MaterialTheme.colorScheme.onSurfaceVariant
    val corDicaFundo = MaterialTheme.colorScheme.inverseSurface
    val corDicaTexto = MaterialTheme.colorScheme.inverseOnSurface
    val estiloRotulo = MaterialTheme.typography.labelMedium
    val rotuloRenda = stringResource(R.string.dashboard_renda)
    val rotuloDespesa = stringResource(R.string.dashboard_despesa)
    val medidor = rememberTextMeasurer()
    var selecionada by remember(grupos) { mutableStateOf<Int?>(null) }

    val valores = grupos.flatMap { listOf(it.rendaCentavos, it.despesaCentavos) }
    val rotulosSerie = listOf(rotuloRenda, rotuloDespesa)
    val descricao = grupos.joinToString(". ") {
        "${it.rotulo}: $rotuloRenda ${Formatadores.moeda(it.rendaCentavos)}, " +
            "$rotuloDespesa ${Formatadores.moeda(it.despesaCentavos)}"
    }

    Column(modifier) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(220.dp)
                .semantics { contentDescription = descricao }
                .pointerInput(grupos) {
                    detectTapGestures { toque ->
                        val indice = areasDasBarras(size.toSize(), grupos.size).indexOfFirst { it.contains(toque) }
                        selecionada = indice.takeIf { it >= 0 && it != selecionada }
                    }
                },
        ) {
            val areas = areasDasBarras(size, grupos.size)
            val maximo = (valores.maxOrNull() ?: 0L).coerceAtLeast(1L)
            val base = areas.firstOrNull()?.bottom ?: return@Canvas
            val raio = CornerRadius(4.dp.toPx())

            areas.forEachIndexed { i, area ->
                val valor = valores[i]
                if (valor <= 0L) return@forEachIndexed
                val altura = (area.height * valor / maximo).coerceAtLeast(2.dp.toPx())
                drawRoundRect(
                    color = if (i % SERIES == 0) corRenda else corDespesa,
                    topLeft = Offset(area.left, base - altura),
                    size = Size(area.width, altura),
                    cornerRadius = raio,
                    alpha = if (selecionada == null || selecionada == i) 1f else 0.4f,
                )
            }
            drawLine(corEixo, Offset(0f, base), Offset(size.width, base), strokeWidth = 1.dp.toPx())

            grupos.forEachIndexed { g, grupo ->
                val texto = medidor.measure(grupo.rotulo, estiloRotulo.copy(color = corRotulo))
                val centro = (areas[g * SERIES].left + areas[g * SERIES + SERIES - 1].right) / 2
                drawText(texto, topLeft = Offset(centro - texto.size.width / 2, base + 6.dp.toPx()))
            }

            selecionada?.let { i ->
                val area = areas[i]
                val valor = valores[i]
                val texto = medidor.measure(
                    "${rotulosSerie[i % SERIES]}: ${Formatadores.moeda(valor)}",
                    estiloRotulo.copy(color = corDicaTexto),
                )
                val margem = 8.dp.toPx()
                val caixa = Size(texto.size.width + 2 * margem, texto.size.height + margem)
                val topoBarra = base - (area.height * valor / maximo)
                val canto = Offset(
                    x = (area.center.x - caixa.width / 2).coerceIn(0f, (size.width - caixa.width).coerceAtLeast(0f)),
                    y = (topoBarra - caixa.height - 4.dp.toPx()).coerceAtLeast(0f),
                )
                drawRoundRect(corDicaFundo, canto, caixa, CornerRadius(6.dp.toPx()))
                drawText(texto, topLeft = canto + Offset(margem, margem / 2))
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth(),
        ) {
            ItemLegenda(corRenda, rotuloRenda)
            ItemLegenda(corDespesa, rotuloDespesa)
        }
        Text(
            stringResource(R.string.dashboard_grafico_dica),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 4.dp),
        )
    }
}

/**
 * Área tocável de cada barra (do topo do gráfico até a linha de base), na ordem
 * grupo 0 renda, grupo 0 despesa, grupo 1 renda…
 */
private fun Density.areasDasBarras(tamanho: Size, quantidadeGrupos: Int): List<Rect> {
    if (quantidadeGrupos == 0) return emptyList()
    val topo = 36.dp.toPx() // espaço para o balão de valor
    val base = tamanho.height - 28.dp.toPx() // espaço para o rótulo do grupo
    val espaco = 8.dp.toPx()
    val larguraGrupo = tamanho.width / quantidadeGrupos
    val larguraBarra = minOf(larguraGrupo * 0.28f, 56.dp.toPx())
    return (0 until quantidadeGrupos).flatMap { g ->
        val inicio = g * larguraGrupo + (larguraGrupo - (SERIES * larguraBarra + (SERIES - 1) * espaco)) / 2
        (0 until SERIES).map { s ->
            val esquerda = inicio + s * (larguraBarra + espaco)
            Rect(esquerda, topo, esquerda + larguraBarra, base)
        }
    }
}

@Composable
private fun ItemLegenda(cor: Color, rotulo: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(12.dp)
                .background(cor, RoundedCornerShape(3.dp)),
        )
        Spacer(Modifier.width(6.dp))
        Text(rotulo, style = MaterialTheme.typography.labelLarge)
    }
}
