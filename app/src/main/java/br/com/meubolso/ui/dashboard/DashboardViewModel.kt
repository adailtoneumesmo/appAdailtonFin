package br.com.meubolso.ui.dashboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.meubolso.data.repository.CartaoRepository
import br.com.meubolso.data.repository.CategoriaRepository
import br.com.meubolso.data.repository.ConfiguracoesRepository
import br.com.meubolso.data.repository.DespesaRepository
import br.com.meubolso.data.repository.NotificacaoBancoRepository
import br.com.meubolso.data.repository.RendaRepository
import br.com.meubolso.domain.dashboard.CalculadoraDashboard
import br.com.meubolso.domain.dashboard.ConsumoCategoria
import br.com.meubolso.domain.dashboard.ResumoDashboard
import br.com.meubolso.domain.model.Cartao
import br.com.meubolso.domain.model.Categoria
import br.com.meubolso.domain.model.CompraCartao
import br.com.meubolso.ui.navigation.Rotas
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.YearMonth
import javax.inject.Inject

data class DashboardUiState(
    val carregando: Boolean = true,
    val mes: YearMonth,
    val resumo: ResumoDashboard = ResumoDashboard(),
    val consumoPorCategoria: List<ConsumoCategoria> = emptyList(),
)

/** Usado pelo Início e pelo detalhamento do mês (que recebe o mês pela rota). */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    rendaRepository: RendaRepository,
    despesaRepository: DespesaRepository,
    cartaoRepository: CartaoRepository,
    categoriaRepository: CategoriaRepository,
    configuracoesRepository: ConfiguracoesRepository,
    notificacaoBancoRepository: NotificacaoBancoRepository,
    private val calculadora: CalculadoraDashboard,
    clock: Clock,
) : ViewModel() {

    private val mes = MutableStateFlow(
        savedStateHandle.get<String>(Rotas.ARG_MES)?.let(YearMonth::parse) ?: YearMonth.now(clock),
    )

    /** Os Flows do Room reemitem a cada inclusão, edição ou exclusão, então o painel se atualiza sozinho. */
    val uiState: StateFlow<DashboardUiState> = combine(
        mes,
        rendaRepository.observarRendas(),
        despesaRepository.observarDespesas(),
        mes.flatMapLatest { despesaRepository.observarPagamentos(it) },
        // combine aceita no máximo 5 Flows tipados; os demais vão agrupados.
        combine(
            configuracoesRepository.anteciparDiaNaoUtil,
            cartaoRepository.observarCartoes(),
            cartaoRepository.observarCompras(),
            categoriaRepository.observarCategorias(),
            ::Complementos,
        ),
    ) { mes, rendas, despesas, pagamentos, (antecipar, cartoes, compras, categorias) ->
        val resumo = calculadora.resumir(rendas, despesas, pagamentos, mes, antecipar, cartoes, compras)
        DashboardUiState(
            carregando = false,
            mes = mes,
            resumo = resumo,
            consumoPorCategoria = resumo.consumoPorCategoria(categorias),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState(mes = mes.value))

    /** Transações lidas das notificações aguardando confirmação. */
    val quantidadePendentes: StateFlow<Int> = notificacaoBancoRepository.observarQuantidadePendentes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun alterarMes(novo: YearMonth) {
        mes.value = novo
    }

    private data class Complementos(
        val anteciparDiaNaoUtil: Boolean,
        val cartoes: List<Cartao>,
        val compras: List<CompraCartao>,
        val categorias: List<Categoria>,
    )
}
