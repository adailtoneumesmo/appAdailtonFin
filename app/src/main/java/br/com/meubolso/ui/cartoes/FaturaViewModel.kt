package br.com.meubolso.ui.cartoes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.meubolso.data.repository.CartaoRepository
import br.com.meubolso.data.repository.CategoriaRepository
import br.com.meubolso.domain.cartao.CalculadoraFatura
import br.com.meubolso.domain.model.Categoria
import br.com.meubolso.domain.model.Fatura
import br.com.meubolso.domain.model.LancamentoFatura
import br.com.meubolso.ui.navigation.Rotas
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class LancamentoUi(
    val lancamento: LancamentoFatura,
    val categoria: Categoria?,
)

data class FaturaUiState(
    val carregando: Boolean = true,
    /** `null` quando o cartão foi excluído. */
    val fatura: Fatura? = null,
    val lancamentos: List<LancamentoUi> = emptyList(),
    val fechada: Boolean = false,
)

@HiltViewModel
class FaturaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val cartaoRepository: CartaoRepository,
    categoriaRepository: CategoriaRepository,
    private val calculadora: CalculadoraFatura,
    private val clock: Clock,
) : ViewModel() {

    private val idCartao: Long = checkNotNull(savedStateHandle.get<Long>(Rotas.ARG_ID))

    /** `null` até o cartão carregar; então abre na fatura que recebe compras feitas hoje. */
    private val mes = MutableStateFlow<YearMonth?>(null)

    val uiState: StateFlow<FaturaUiState> = combine(
        cartaoRepository.observarCartao(idCartao),
        cartaoRepository.observarCompras(idCartao),
        categoriaRepository.observarCategorias(),
        mes,
    ) { cartao, compras, categorias, mesSelecionado ->
        if (cartao == null) return@combine FaturaUiState(carregando = false)
        val hoje = LocalDate.now(clock)
        val mesFatura = mesSelecionado ?: calculadora.faturaDaCompra(cartao, hoje)
        val fatura = calculadora.fatura(cartao, compras, mesFatura)
        val categoriasPorId = categorias.associateBy { it.id }
        FaturaUiState(
            carregando = false,
            fatura = fatura,
            lancamentos = fatura.lancamentos.map { LancamentoUi(it, categoriasPorId[it.compra.categoriaId]) },
            fechada = fatura.fechada(hoje),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FaturaUiState())

    init {
        viewModelScope.launch {
            val cartao = cartaoRepository.observarCartao(idCartao).filterNotNull().first()
            mes.compareAndSet(null, calculadora.faturaDaCompra(cartao, LocalDate.now(clock)))
        }
    }

    fun alterarMes(novo: YearMonth) {
        mes.value = novo
    }
}
