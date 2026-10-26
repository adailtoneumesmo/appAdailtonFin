package br.com.meubolso.ui.listacompras

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.meubolso.data.repository.ListaCompraRepository
import br.com.meubolso.domain.listacompra.CalculadoraListaCompra
import br.com.meubolso.domain.listacompra.ResumoListaCompra
import br.com.meubolso.domain.model.ListaCompra
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class ListaCompraCardUi(
    val lista: ListaCompra,
    val resumo: ResumoListaCompra,
)

data class ListasCompraUiState(
    val carregando: Boolean = true,
    val mes: YearMonth,
    val listas: List<ListaCompraCardUi> = emptyList(),
) {
    val totalMesCentavos: Long get() = listas.sumOf { it.resumo.totalCentavos }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ListasCompraViewModel @Inject constructor(
    private val repository: ListaCompraRepository,
    private val clock: Clock,
) : ViewModel() {

    private val mes = MutableStateFlow(YearMonth.now(clock))

    /** Mês para onde uma lista foi duplicada, para avisar o usuário. */
    private val _duplicadas = Channel<YearMonth>(Channel.BUFFERED)
    val duplicadas: Flow<YearMonth> = _duplicadas.receiveAsFlow()

    val uiState: StateFlow<ListasCompraUiState> = mes.flatMapLatest { m ->
        combine(repository.observarListas(m), repository.observarItensDoMes(m)) { listas, itens ->
            val itensPorLista = itens.groupBy { it.listaId }
            ListasCompraUiState(
                carregando = false,
                mes = m,
                listas = listas.map { lista ->
                    ListaCompraCardUi(
                        lista = lista,
                        resumo = CalculadoraListaCompra.resumir(itensPorLista[lista.id].orEmpty(), lista.limiteCentavos),
                    )
                },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ListasCompraUiState(mes = mes.value))

    fun alterarMes(novo: YearMonth) {
        mes.value = novo
    }

    fun excluir(lista: ListaCompra) {
        viewModelScope.launch { repository.excluirLista(lista.id) }
    }

    /** Duplica com a data de hoje e mostra o mês atual, onde a cópia aparece. */
    fun duplicar(lista: ListaCompra) {
        viewModelScope.launch {
            val hoje = LocalDate.now(clock)
            repository.duplicar(lista.id, hoje)
            mes.value = YearMonth.from(hoje)
            _duplicadas.send(YearMonth.from(hoje))
        }
    }
}
