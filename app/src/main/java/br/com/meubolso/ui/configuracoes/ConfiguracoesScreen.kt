package br.com.meubolso.ui.configuracoes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.meubolso.R
import br.com.meubolso.ui.components.LogoMeuBolso

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracoesScreen(
    onCategorias: () -> Unit,
    viewModel: ConfiguracoesViewModel = hiltViewModel(),
) {
    val antecipar by viewModel.anteciparDiaNaoUtil.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.configuracoes_titulo)) }) },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            LogoMeuBolso(
                altura = 36.dp,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 24.dp),
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.config_antecipar_titulo)) },
                supportingContent = { Text(stringResource(R.string.config_antecipar_descricao)) },
                trailingContent = {
                    Switch(checked = antecipar, onCheckedChange = viewModel::alterarAnteciparDiaNaoUtil)
                },
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.config_categorias_titulo)) },
                supportingContent = { Text(stringResource(R.string.config_categorias_descricao)) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onCategorias),
            )
        }
    }
}
