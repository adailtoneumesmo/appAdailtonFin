package br.com.meubolso.ui.objetivos

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.com.meubolso.R
import br.com.meubolso.domain.objetivo.ProgressoObjetivo
import br.com.meubolso.domain.objetivo.ResumoObjetivos
import br.com.meubolso.ui.dashboard.FatiaGrafico
import br.com.meubolso.ui.dashboard.GraficoConsumo
import br.com.meubolso.util.Formatadores
import kotlin.math.roundToInt

/** Soma de todos os objetivos: valores em destaque e a rosca realizado x falta. */
@Composable
fun ResumoTotalObjetivos(resumo: ResumoObjetivos, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            stringResource(R.string.objetivos_progresso, Formatadores.moeda(resumo.realizadoCentavos), Formatadores.moeda(resumo.alvoCentavos)),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            stringResource(R.string.objetivos_percentual_atingido, percentual(resumo.fracao)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        GraficoConsumo(
            fatias = listOf(
                FatiaGrafico(stringResource(R.string.objetivos_realizado), MaterialTheme.colorScheme.primary, resumo.realizadoCentavos),
                FatiaGrafico(stringResource(R.string.objetivos_falta), MaterialTheme.colorScheme.outline, resumo.faltaCentavos),
            ).filter { it.centavos > 0 },
            rotuloTotal = stringResource(R.string.objetivos_meta_total),
            modifier = Modifier.padding(top = 16.dp),
        )
        if (resumo.excedenteCentavos > 0) {
            Text(
                stringResource(R.string.objetivos_excedente, Formatadores.moeda(resumo.excedenteCentavos)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

/** Barra de progresso de um objetivo, com o texto do que falta (ou de concluído). */
@Composable
fun BarraObjetivo(item: ProgressoObjetivo) {
    LinearProgressIndicator(
        progress = { item.fracao.toFloat() },
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.outlineVariant,
        strokeCap = StrokeCap.Round,
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp),
    )
    Text(
        if (item.concluido) {
            stringResource(R.string.objetivos_concluido)
        } else {
            stringResource(R.string.objetivos_faltam, Formatadores.moeda(item.faltaCentavos)) +
                " · " + stringResource(R.string.objetivos_percentual_atingido, percentual(item.fracao))
        },
        style = MaterialTheme.typography.bodySmall,
        color = if (item.concluido) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )
}

private fun percentual(fracao: Double): Int = (fracao * 100).roundToInt()
