package br.com.meubolso.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.meubolso.data.repository.CartaoRepository
import br.com.meubolso.data.repository.ConfiguracoesRepository
import br.com.meubolso.data.repository.DespesaRepository
import br.com.meubolso.data.repository.RendaRepository
import br.com.meubolso.domain.dashboard.CalculadoraDashboard
import br.com.meubolso.domain.dashboard.ResumoDashboard
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
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    rendaRepository: RendaRepository,
    despesaRepository: DespesaRepository,
    cartaoRepository: CartaoRepository,
    configuracoesRepository: ConfiguracoesRepository,
    private val calculadora: CalculadoraDashboard,
    clock: Clock,
) : ViewModel() {

    private val mes = MutableStateFlow(YearMonth.now(clock))

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
            ::Triple,
        ),
    ) { mes, rendas, despesas, pagamentos, (antecipar, cartoes, compras) ->
        DashboardUiState(
            carregando = false,
            mes = mes,
            resumo = calculadora.resumir(rendas, despesas, pagamentos, mes, antecipar, cartoes, compras),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState(mes = mes.value))

    fun alterarMes(novo: YearMonth) {
        mes.value = novo
    }
}
