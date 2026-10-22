package br.com.meubolso.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CoresClaras = lightColorScheme(
    primary = Color(0xFF1B7F4B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA6F2C3),
    onPrimaryContainer = Color(0xFF00210F),
    secondary = Color(0xFF4E6354),
    secondaryContainer = Color(0xFFD0E8D5),
    tertiary = Color(0xFF3B6470),
    error = Color(0xFFBA1A1A),
    background = Color(0xFFF6FBF4),
    surface = Color(0xFFF6FBF4),
)

private val CoresEscuras = darkColorScheme(
    primary = Color(0xFF8BD5A8),
    onPrimary = Color(0xFF00391D),
    primaryContainer = Color(0xFF00522C),
    onPrimaryContainer = Color(0xFFA6F2C3),
    secondary = Color(0xFFB4CCB9),
    secondaryContainer = Color(0xFF364B3D),
    tertiary = Color(0xFFA3CDDB),
    error = Color(0xFFFFB4AB),
    background = Color(0xFF0F1511),
    surface = Color(0xFF0F1511),
)

@Composable
fun MeuBolsoTheme(
    escuro: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (escuro) CoresEscuras else CoresClaras,
        content = content,
    )
}
