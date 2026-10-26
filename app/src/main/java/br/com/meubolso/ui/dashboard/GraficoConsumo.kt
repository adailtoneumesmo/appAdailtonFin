package br.com.meubolso.ui.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.meubolso.R
import br.com.meubolso.util.Formatadores
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

data class FatiaGrafico(val rotulo: String, val cor: Color, val centavos: Long)

private const val GRAUS_VOLTA = 360f
private const val INICIO_GRAUS = -90f // 12 horas
private const val ESPACO_GRAUS = 2f

/**
 * Rosca com a participação de cada fatia no total, mais a legenda com valor e percentual.
 * Tocar numa fatia (ou na linha da legenda) destaca ela e mostra seus números no centro.
 */
@Composable
fun GraficoConsumo(fatias: List<FatiaGrafico>, modifier: Modifier = Modifier) {
    val total = fatias.sumOf { it.centavos }.coerceAtLeast(1L)
    var selecionada by remember(fatias) { mutableStateOf<Int?>(null) }
    val progresso = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progresso.animateTo(1f, tween(durationMillis = 800, easing = FastOutSlowInEasing)) }

    val corTrilho = MaterialTheme.colorScheme.surfaceVariant
    val descricao = fatias.joinToString(". ") { "${it.rotulo}: ${Formatadores.moeda(it.centavos)}" }
    val varreduras = fatias.map { GRAUS_VOLTA * it.centavos / total }
    val espaco = if (fatias.size > 1) ESPACO_GRAUS else 0f

    Column(modifier) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(220.dp),
        ) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .semantics { contentDescription = descricao }
                    .pointerInput(fatias) {
                        detectTapGestures { toque ->
                            val centro = Offset(size.width / 2f, size.height / 2f)
                            val raioExterno = size.width / 2f
                            val distancia = hypot(toque.x - centro.x, toque.y - centro.y)
                            if (distancia > raioExterno || distancia < raioExterno * 0.55f) {
                                selecionada = null
                                return@detectTapGestures
                            }
                            val graus = Math.toDegrees(atan2(toque.y - centro.y, toque.x - centro.x).toDouble()).toFloat()
                            val angulo = (graus - INICIO_GRAUS + GRAUS_VOLTA) % GRAUS_VOLTA
                            var acumulado = 0f
                            val indice = varreduras.indexOfFirst { v -> (angulo < acumulado + v).also { acumulado += v } }
                            selecionada = indice.takeIf { it >= 0 && it != selecionada }
                        }
                    },
            ) {
                val espessura = 26.dp.toPx()
                val destaque = 8.dp.toPx()
                val diametro = size.minDimension - espessura - destaque
                val canto = Offset((size.width - diametro) / 2f, (size.height - diametro) / 2f)
                val tamanho = Size(diametro, diametro)

                drawArc(corTrilho, 0f, GRAUS_VOLTA, useCenter = false, topLeft = canto, size = tamanho, style = Stroke(espessura))

                var inicio = INICIO_GRAUS
                fatias.forEachIndexed { i, fatia ->
                    val varredura = varreduras[i] * progresso.value
                    val destacada = selecionada == i
                    drawArc(
                        color = fatia.cor,
                        startAngle = inicio + espaco / 2,
                        sweepAngle = (varredura - espaco).coerceAtLeast(0.5f),
                        useCenter = false,
                        topLeft = canto,
                        size = tamanho,
                        alpha = if (selecionada == null || destacada) 1f else 0.3f,
                        style = Stroke(if (destacada) espessura + destaque else espessura),
                    )
                    inicio += varredura
                }
            }
            CentroGrafico(fatias.getOrNull(selecionada ?: -1), total)
        }

        Spacer(Modifier.height(16.dp))
        fatias.forEachIndexed { i, fatia ->
            LinhaLegenda(
                fatia = fatia,
                percentual = percentual(fatia.centavos, total),
                esmaecida = selecionada != null && selecionada != i,
                onClick = { selecionada = i.takeIf { it != selecionada } },
            )
        }
    }
}

@Composable
private fun CentroGrafico(fatia: FatiaGrafico?, total: Long) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(130.dp)) {
        Text(
            fatia?.rotulo ?: stringResource(R.string.dashboard_total_despesas),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            Formatadores.moeda(fatia?.centavos ?: total),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        if (fatia != null) {
            Text(
                stringResource(R.string.dashboard_percentual, percentual(fatia.centavos, total)),
                style = MaterialTheme.typography.labelMedium,
                color = fatia.cor,
            )
        }
    }
}

@Composable
private fun LinhaLegenda(fatia: FatiaGrafico, percentual: Int, esmaecida: Boolean, onClick: () -> Unit) {
    val corTexto = if (esmaecida) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
    ) {
        Box(
            Modifier
                .size(12.dp)
                .background(fatia.cor.copy(alpha = if (esmaecida) 0.3f else 1f), CircleShape),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            fatia.rotulo,
            style = MaterialTheme.typography.bodyMedium,
            color = corTexto,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            stringResource(R.string.dashboard_percentual, percentual),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            Formatadores.moeda(fatia.centavos),
            style = MaterialTheme.typography.titleSmall,
            color = corTexto,
            textAlign = TextAlign.End,
            modifier = Modifier.width(104.dp),
        )
    }
}

private fun percentual(centavos: Long, total: Long): Int = (centavos * 100.0 / total).roundToInt()
