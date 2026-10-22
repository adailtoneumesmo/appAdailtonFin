package br.com.meubolso.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Círculo com a cor da categoria. */
@Composable
fun PontoCor(cor: Int, modifier: Modifier = Modifier, tamanho: Dp = 12.dp) {
    Box(
        modifier
            .size(tamanho)
            .background(Color(cor), CircleShape),
    )
}
