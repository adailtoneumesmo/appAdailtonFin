package br.com.meubolso.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.meubolso.R
import br.com.meubolso.ui.theme.LocalTemaEscuro

/** Logotipo "meu bolso"; usa a versão negativa no tema escuro. */
@Composable
fun LogoMeuBolso(modifier: Modifier = Modifier, altura: Dp = 28.dp) {
    Image(
        painter = painterResource(
            if (LocalTemaEscuro.current) R.drawable.logo_meubolso_negativo else R.drawable.logo_meubolso,
        ),
        contentDescription = stringResource(R.string.app_name),
        modifier = modifier.height(altura),
    )
}
