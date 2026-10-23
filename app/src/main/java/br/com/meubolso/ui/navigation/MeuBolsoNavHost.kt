package br.com.meubolso.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.com.meubolso.R
import br.com.meubolso.ui.cartoes.CartaoFormScreen
import br.com.meubolso.ui.cartoes.CartoesScreen
import br.com.meubolso.ui.cartoes.FaturaScreen
import br.com.meubolso.ui.categorias.CategoriasScreen
import br.com.meubolso.ui.configuracoes.ConfiguracoesScreen
import br.com.meubolso.ui.dashboard.DashboardScreen
import br.com.meubolso.ui.despesas.DespesaFormScreen
import br.com.meubolso.ui.despesas.DespesasScreen
import br.com.meubolso.ui.rendas.RendaFormScreen
import br.com.meubolso.ui.rendas.RendasScreen

object Rotas {
    const val ARG_ID = "id"
    const val ARG_COMPRA_ID = "compraId"
    const val ARG_CARTAO_ID = "cartaoId"
    private const val SEM_ID = -1L

    const val DASHBOARD = "dashboard"
    const val DESPESAS = "despesas"
    const val DESPESA_FORM = "despesas/form?$ARG_ID={$ARG_ID}&$ARG_COMPRA_ID={$ARG_COMPRA_ID}&$ARG_CARTAO_ID={$ARG_CARTAO_ID}"
    const val CATEGORIAS = "categorias"
    const val RENDAS = "rendas"
    const val RENDA_FORM = "rendas/form?$ARG_ID={$ARG_ID}"
    const val CARTOES = "cartoes"
    const val CARTAO_FORM = "cartoes/form?$ARG_ID={$ARG_ID}"
    const val FATURA = "cartoes/fatura?$ARG_ID={$ARG_ID}"
    const val CONFIGURACOES = "configuracoes"

    fun despesaForm(id: Long? = null) = "despesas/form?$ARG_ID=${id ?: SEM_ID}"

    /** Compra no cartão: edita [compraId] ou cria uma nova já com o cartão [cartaoId] selecionado. */
    fun compraForm(compraId: Long? = null, cartaoId: Long? = null) =
        "despesas/form?$ARG_COMPRA_ID=${compraId ?: SEM_ID}&$ARG_CARTAO_ID=${cartaoId ?: SEM_ID}"

    fun rendaForm(id: Long? = null) = "rendas/form?$ARG_ID=${id ?: SEM_ID}"

    fun cartaoForm(id: Long? = null) = "cartoes/form?$ARG_ID=${id ?: SEM_ID}"

    fun fatura(cartaoId: Long) = "cartoes/fatura?$ARG_ID=$cartaoId"

    val argumentoId = argumentoLong(ARG_ID)

    val argumentosDespesaForm = listOf(argumentoId, argumentoLong(ARG_COMPRA_ID), argumentoLong(ARG_CARTAO_ID))

    private fun argumentoLong(nome: String) = navArgument(nome) {
        type = NavType.LongType
        defaultValue = SEM_ID
    }
}

/** Destinos da barra de navegação inferior. */
enum class Aba(val rota: String, @StringRes val rotulo: Int, @DrawableRes val icone: Int) {
    INICIO(Rotas.DASHBOARD, R.string.aba_inicio, R.drawable.ic_aba_inicio),
    DESPESAS(Rotas.DESPESAS, R.string.aba_despesas, R.drawable.ic_aba_despesas),
    CARTOES(Rotas.CARTOES, R.string.aba_cartoes, R.drawable.ic_aba_cartoes),
    RENDAS(Rotas.RENDAS, R.string.aba_rendas, R.drawable.ic_aba_rendas),
    CONFIGURACOES(Rotas.CONFIGURACOES, R.string.aba_configuracoes, R.drawable.ic_aba_configuracoes),
}

@Composable
fun MeuBolsoNavHost() {
    val navController = rememberNavController()
    val entradaAtual by navController.currentBackStackEntryAsState()
    val destinoAtual = entradaAtual?.destination
    val exibirBarra = Aba.entries.any { aba -> destinoAtual?.route == aba.rota }

    Scaffold(
        bottomBar = {
            if (exibirBarra) {
                NavigationBar {
                    Aba.entries.forEach { aba ->
                        NavigationBarItem(
                            selected = destinoAtual?.hierarchy?.any { it.route == aba.rota } == true,
                            onClick = { navController.navegarParaAba(aba) },
                            icon = { Icon(painterResource(aba.icone), contentDescription = null) },
                            label = { Text(stringResource(aba.rotulo)) },
                        )
                    }
                }
            }
        },
        // Cada tela tem o próprio Scaffold; aqui só reservamos o espaço da barra inferior.
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Rotas.DASHBOARD,
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding),
        ) {
            composable(Rotas.DASHBOARD) {
                DashboardScreen(
                    onNovaRenda = { navController.navigate(Rotas.rendaForm()) },
                    onNovaDespesa = { navController.navigate(Rotas.despesaForm()) },
                    onEditarRenda = { navController.navigate(Rotas.rendaForm(it)) },
                    onEditarDespesa = { navController.navigate(Rotas.despesaForm(it)) },
                    onAbrirFatura = { navController.navigate(Rotas.fatura(it)) },
                )
            }
            composable(Rotas.DESPESAS) {
                DespesasScreen(
                    onNovaDespesa = { navController.navigate(Rotas.despesaForm()) },
                    onEditarDespesa = { navController.navigate(Rotas.despesaForm(it)) },
                )
            }
            composable(Rotas.DESPESA_FORM, arguments = Rotas.argumentosDespesaForm) {
                DespesaFormScreen(
                    onVoltar = { navController.popBackStack() },
                    onGerenciarCategorias = { navController.navigate(Rotas.CATEGORIAS) },
                    onNovoCartao = { navController.navigate(Rotas.cartaoForm()) },
                )
            }
            composable(Rotas.CARTOES) {
                CartoesScreen(
                    onNovoCartao = { navController.navigate(Rotas.cartaoForm()) },
                    onEditarCartao = { navController.navigate(Rotas.cartaoForm(it)) },
                    onAbrirFatura = { navController.navigate(Rotas.fatura(it)) },
                )
            }
            composable(Rotas.CARTAO_FORM, arguments = listOf(Rotas.argumentoId)) {
                CartaoFormScreen(onVoltar = { navController.popBackStack() })
            }
            composable(Rotas.FATURA, arguments = listOf(Rotas.argumentoId)) {
                FaturaScreen(
                    onVoltar = { navController.popBackStack() },
                    onEditarCartao = { navController.navigate(Rotas.cartaoForm(it)) },
                    onNovaDespesa = { navController.navigate(Rotas.compraForm(cartaoId = it)) },
                    onEditarDespesa = { navController.navigate(Rotas.compraForm(compraId = it)) },
                )
            }
            composable(Rotas.CATEGORIAS) {
                CategoriasScreen(onVoltar = { navController.popBackStack() })
            }
            composable(Rotas.RENDAS) {
                RendasScreen(
                    onNovaRenda = { navController.navigate(Rotas.rendaForm()) },
                    onEditarRenda = { navController.navigate(Rotas.rendaForm(it)) },
                )
            }
            composable(Rotas.RENDA_FORM, arguments = listOf(Rotas.argumentoId)) {
                RendaFormScreen(onVoltar = { navController.popBackStack() })
            }
            composable(Rotas.CONFIGURACOES) {
                ConfiguracoesScreen(onCategorias = { navController.navigate(Rotas.CATEGORIAS) })
            }
        }
    }
}

/** Troca de aba preservando o estado de cada uma e sem empilhar destinos repetidos. */
private fun NavHostController.navegarParaAba(aba: Aba) {
    navigate(aba.rota) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
