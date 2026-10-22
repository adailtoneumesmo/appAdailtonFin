package br.com.meubolso.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.com.meubolso.ui.configuracoes.ConfiguracoesScreen
import br.com.meubolso.ui.rendas.RendaFormScreen
import br.com.meubolso.ui.rendas.RendasScreen

object Rotas {
    const val ARG_ID = "id"
    private const val SEM_ID = -1L

    const val RENDAS = "rendas"
    const val RENDA_FORM = "rendas/form?$ARG_ID={$ARG_ID}"
    const val CONFIGURACOES = "configuracoes"

    fun rendaForm(id: Long? = null) = "rendas/form?$ARG_ID=${id ?: SEM_ID}"

    val argumentoId = navArgument(ARG_ID) {
        type = NavType.LongType
        defaultValue = SEM_ID
    }
}

@Composable
fun MeuBolsoNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Rotas.RENDAS) {
        composable(Rotas.RENDAS) {
            RendasScreen(
                onNovaRenda = { navController.navigate(Rotas.rendaForm()) },
                onEditarRenda = { navController.navigate(Rotas.rendaForm(it)) },
                onConfiguracoes = { navController.navigate(Rotas.CONFIGURACOES) },
            )
        }
        composable(Rotas.RENDA_FORM, arguments = listOf(Rotas.argumentoId)) {
            RendaFormScreen(onVoltar = { navController.popBackStack() })
        }
        composable(Rotas.CONFIGURACOES) {
            ConfiguracoesScreen(onVoltar = { navController.popBackStack() })
        }
    }
}
