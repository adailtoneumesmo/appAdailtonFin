package br.com.meubolso.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Cores da marca (meu-bolso-logo). */
object CoresMarca {
    val Verde = Color(0xFF127A45)
    val VerdeEscuro = Color(0xFF0E3B24)
    val Amarelo = Color(0xFFFFC533)
}

private val CoresClaras = lightColorScheme(
    primary = CoresMarca.Verde,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD2F5DF),
    onPrimaryContainer = CoresMarca.VerdeEscuro,
    secondary = CoresMarca.Amarelo,
    onSecondary = CoresMarca.VerdeEscuro,
    secondaryContainer = Color(0xFFFFEDB8),
    onSecondaryContainer = Color(0xFF3D2E00),
    tertiary = CoresMarca.VerdeEscuro,
    onTertiary = Color.White,
    error = Color(0xFFBA1A1A),
    background = Color(0xFFF7FBF7),
    onBackground = Color(0xFF17201A),
    surface = Color(0xFFF7FBF7),
    onSurface = Color(0xFF17201A),
    surfaceVariant = Color(0xFFDCE5DC),
    onSurfaceVariant = Color(0xFF414941),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF1F5F1),
    surfaceContainer = Color(0xFFEBF0EB),
    surfaceContainerHigh = Color(0xFFE5EAE5),
    surfaceContainerHighest = Color(0xFFDFE4DF),
    outline = Color(0xFF717970),
    outlineVariant = Color(0xFFC1C9C0),
)

private val CoresEscuras = darkColorScheme(
    primary = Color(0xFF7FD8A3),
    onPrimary = Color(0xFF003920),
    primaryContainer = CoresMarca.Verde,
    onPrimaryContainer = Color(0xFFE6FFEE),
    secondary = CoresMarca.Amarelo,
    onSecondary = Color(0xFF3D2E00),
    secondaryContainer = Color(0xFF5A4400),
    onSecondaryContainer = Color(0xFFFFEDB8),
    tertiary = Color(0xFFA6D1B6),
    onTertiary = CoresMarca.VerdeEscuro,
    error = Color(0xFFFFB4AB),
    background = Color(0xFF0E1A13),
    onBackground = Color(0xFFDDE5DD),
    surface = Color(0xFF0E1A13),
    onSurface = Color(0xFFDDE5DD),
    surfaceVariant = Color(0xFF414941),
    onSurfaceVariant = Color(0xFFC1C9C0),
    surfaceContainerLowest = Color(0xFF09140E),
    surfaceContainerLow = Color(0xFF16211A),
    surfaceContainer = Color(0xFF1A251E),
    surfaceContainerHigh = Color(0xFF243029),
    surfaceContainerHighest = Color(0xFF2F3B33),
    outline = Color(0xFF8B938A),
    outlineVariant = Color(0xFF414941),
)

/** Indica se o tema escuro está ativo (para escolher a versão negativa do logo). */
val LocalTemaEscuro = staticCompositionLocalOf { false }

@Composable
fun MeuBolsoTheme(
    escuro: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalTemaEscuro provides escuro) {
        MaterialTheme(
            colorScheme = if (escuro) CoresEscuras else CoresClaras,
            content = content,
        )
    }
}
