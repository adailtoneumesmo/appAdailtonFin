package br.com.meubolso.ui.listacompras

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.meubolso.data.repository.DespesaRepository
import br.com.meubolso.data.repository.ListaCompraRepository
import br.com.meubolso.data.repository.RendaRepository
import br.com.meubolso.domain.listacompra.CampoListaCompra
import br.com.meubolso.domain.listacompra.ValidadorListaCompra
import br.com.meubolso.domain.model.ListaCompra
import br.com.meubolso.domain.vale.CalculadoraValeAlimentacao
import br.com.meubolso.domain.vale.SaldoVale
import br.com.meubolso.domain.validacao.ErroValidacao
import br.com.meubolso.ui.navigation.Rotas
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class ListaCompraFormUiState(
    val id: Long? = null,
    val descricao: String = "",
    val data: LocalDate? = null,
    /** 0 = sem limite. */
    val limiteCentavos: Long = 0,
    /** Mês do VR/VA usado para preencher o limite; some quando o usuário altera o valor. */
    val limiteDoVale: YearMonth? = null,
    val erros: Map<CampoListaCompra, ErroValidacao> = emptyMap(),
    /** Id da lista salva; a tela navega quando deixa de ser nulo. */
    val salvaId: Long? = null,
) {
    val editando: Boolean get() = id != null
}

/** Saldo do VR/VA no mês da lista, para sugerir o limite de gasto. */
data class SugestaoVale(val mes: YearMonth, val saldo: SaldoVale) {
    val possuiCredito: Boolean get() = saldo.creditos.isNotEmpty()

    /** Sem despesas é o valor projetado; com despesas, o que sobra (nunca negativo). */
    val limiteSugeridoCentavos: Long get() = saldo.saldoCentavos.coerceAtLeast(0)
}

@HiltViewModel
class ListaCompraFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ListaCompraRepository,
    rendaRepository: RendaRepository,
    despesaRepository: DespesaRepository,
    private val calculadoraVale: CalculadoraValeAlimentacao,
    clock: Clock,
) : ViewModel() {

    private val idLista: Long? = savedStateHandle.get<Long>(Rotas.ARG_ID)?.takeIf { it > 0 }

    /** Preservado na edição: editar não desfaz a finalização. */
    private var original: ListaCompra? = null

    private val _uiState = MutableStateFlow(ListaCompraFormUiState(id = idLista, data = LocalDate.now(clock)))
    val uiState: StateFlow<ListaCompraFormUiState> = _uiState.asStateFlow()

    val sugestaoVale: StateFlow<SugestaoVale?> = combine(
        _uiState,
        rendaRepository.observarRendas(),
        despesaRepository.observarDespesas(),
    ) { estado, rendas, despesas ->
        val mes = YearMonth.from(estado.data ?: return@combine null)
        SugestaoVale(mes, calculadoraVale.saldoNoMes(rendas, despesas, emptyList(), mes))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        if (idLista != null) {
            viewModelScope.launch {
                repository.buscarLista(idLista)?.let { lista ->
                    original = lista
                    _uiState.update {
                        it.copy(
                            descricao = lista.descricao,
                            data = lista.data,
                            limiteCentavos = lista.limiteCentavos ?: 0,
                        )
                    }
                }
            }
        }
    }

    fun alterarDescricao(valor: String) = atualizar(CampoListaCompra.DESCRICAO) { copy(descricao = valor) }

    fun alterarData(valor: LocalDate?) = atualizar(CampoListaCompra.DATA) { copy(data = valor) }

    fun alterarLimite(centavos: Long) = _uiState.update { it.copy(limiteCentavos = centavos, limiteDoVale = null) }

    fun usarSaldoVale() {
        val sugestao = sugestaoVale.value ?: return
        _uiState.update { it.copy(limiteCentavos = sugestao.limiteSugeridoCentavos, limiteDoVale = sugestao.mes) }
    }

    fun salvar() {
        val estado = _uiState.value
        val erros = ValidadorListaCompra.validarLista(estado.descricao, estado.data)
        if (erros.isNotEmpty()) {
            _uiState.update { it.copy(erros = erros) }
            return
        }
        val base = original ?: ListaCompra(descricao = "", data = checkNotNull(estado.data))
        val lista = base.copy(
            descricao = estado.descricao.trim(),
            data = checkNotNull(estado.data),
            limiteCentavos = estado.limiteCentavos.takeIf { it > 0 },
        )
        viewModelScope.launch {
            val id = repository.salvarLista(lista)
            _uiState.update { it.copy(salvaId = id) }
        }
    }

    /** Aplica a alteração e limpa o erro dos campos afetados. */
    private fun atualizar(vararg campos: CampoListaCompra, alteracao: ListaCompraFormUiState.() -> ListaCompraFormUiState) {
        _uiState.update { it.alteracao().copy(erros = it.erros - campos.toSet()) }
    }
}
