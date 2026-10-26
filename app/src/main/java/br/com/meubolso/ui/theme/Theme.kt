package br.com.meubolso.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Cores da marca (meu-bolso-logo-v2). */
object CoresMarca {
    val Tinta = Color(0xFF0E1512)
    val Limao = Color(0xFFC8F560)
    val LimaoEscuro = Color(0xFF7CB518)
    val Claro = Color(0xFFF5F6FA)

    /** Faixa de alerta do consumo; fora da paleta da marca para não se confundir com o limão. */
    val Alerta = Color(0xFFF2B705)
}

/**
 * No tema claro o limão não tem contraste para texto, então [primary] (usado em valores
 * positivos, botões e destaques) é um verde-limão escurecido; o limão fica nos containers.
 */
private val CoresClaras = lightColorScheme(
    primary = Color(0xFF3F6A00),
    onPrimary = Color.White,
    primaryContainer = CoresMarca.Limao,
    onPrimaryContainer = CoresMarca.Tinta,
    secondary = CoresMarca.Tinta,
    onSecondary = CoresMarca.Limao,
    secondaryContainer = Color(0xFFE3E7EC),
    onSecondaryContainer = CoresMarca.Tinta,
    tertiary = CoresMarca.LimaoEscuro,
    onTertiary = CoresMarca.Tinta,
    error = Color(0xFFBA1A1A),
    background = CoresMarca.Claro,
    onBackground = CoresMarca.Tinta,
    surface = CoresMarca.Claro,
    onSurface = CoresMarca.Tinta,
    surfaceVariant = Color(0xFFE1E4EA),
    onSurfaceVariant = Color(0xFF4A524E),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFBFBFD),
    surfaceContainer = Color(0xFFEFF1F5),
    surfaceContainerHigh = Color(0xFFE9EBF0),
    // Card preenchido usa este tom: cards brancos sobre o fundo claro.
    surfaceContainerHighest = Color.White,
    outline = Color(0xFF78807C),
    outlineVariant = Color(0xFFD3D7DE),
)

private val CoresEscuras = darkColorScheme(
    primary = CoresMarca.Limao,
    onPrimary = CoresMarca.Tinta,
    primaryContainer = Color(0xFF2C4210),
    onPrimaryContainer = Color(0xFFE4FBB0),
    secondary = CoresMarca.Claro,
    onSecondary = CoresMarca.Tinta,
    secondaryContainer = Color(0xFF242E29),
    onSecondaryContainer = CoresMarca.Claro,
    tertiary = CoresMarca.LimaoEscuro,
    onTertiary = CoresMarca.Tinta,
    error = Color(0xFFFFB4AB),
    background = CoresMarca.Tinta,
    onBackground = CoresMarca.Claro,
    surface = CoresMarca.Tinta,
    onSurface = CoresMarca.Claro,
    surfaceVariant = Color(0xFF2A332F),
    onSurfaceVariant = Color(0xFFB9C2BD),
    surfaceContainerLowest = Color(0xFF0A100D),
    surfaceContainerLow = Color(0xFF141C18),
    surfaceContainer = Color(0xFF18211C),
    surfaceContainerHigh = Color(0xFF1B2420),
    surfaceContainerHighest = Color(0xFF1F2924),
    outline = Color(0xFF87908B),
    outlineVariant = Color(0xFF39433E),
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
