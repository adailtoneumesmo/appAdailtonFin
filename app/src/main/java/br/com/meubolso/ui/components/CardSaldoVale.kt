package br.com.meubolso.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.com.meubolso.R
import br.com.meubolso.domain.vale.SaldoVale
import br.com.meubolso.domain.vale.ValorVale
import br.com.meubolso.util.Formatadores

/** Carteira do VR/VA no mês, exibida à parte do saldo de salário/vale. */
@Composable
fun CardSaldoVale(saldo: SaldoVale, modifier: Modifier = Modifier, titulo: String = stringResource(R.string.saldo_vale_titulo)) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.saldo_vale_credito), style = MaterialTheme.typography.labelMedium)
                    Text(Formatadores.moeda(saldo.creditoCentavos), style = MaterialTheme.typography.titleMedium)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.saldo_vale_gasto), style = MaterialTheme.typography.labelMedium)
                    Text(Formatadores.moeda(saldo.gastoCentavos), style = MaterialTheme.typography.titleMedium)
                }
            }
            saldo.creditos.mapNotNull { it.valor.takeIf { v -> v.diasUteis != null } }.forEach {
                Text(
                    textoCalculoVale(it),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            Text(stringResource(R.string.saldo_vale_disponivel), style = MaterialTheme.typography.labelMedium)
            Text(
                Formatadores.moeda(saldo.saldoCentavos),
                style = MaterialTheme.typography.headlineSmall,
                color = if (saldo.saldoCentavos < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
            Text(
                stringResource(R.string.saldo_vale_separado),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** "19 dias úteis em novembro/2026 × R$ 20,00 = R$ 380,00"; no valor fixo, só o valor. */
@Composable
fun textoCalculoVale(valor: ValorVale): String {
    val dias = valor.diasUteis
    val mes = valor.mesDiasUteis
    val diario = valor.valorDiarioCentavos
    return if (dias != null && mes != null && diario != null) {
        pluralStringResource(
            R.plurals.vale_calculo_por_dia,
            dias,
            dias,
            Formatadores.mesAnoCompacto(mes),
            Formatadores.moeda(diario),
            Formatadores.moeda(valor.totalCentavos),
        )
    } else {
        Formatadores.moeda(valor.totalCentavos)
    }
}
