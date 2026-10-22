package br.com.meubolso.ui.rendas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.meubolso.data.repository.ConfiguracoesRepository
import br.com.meubolso.data.repository.RendaRepository
import br.com.meubolso.domain.model.Renda
import br.com.meubolso.domain.renda.CalculadoraRecebimento
import br.com.meubolso.domain.renda.DataRecebimento
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

data class RendaItemUi(
    val renda: Renda,
    val proximoRecebimento: DataRecebimento?,
)

data class RendasUiState(
    val carregando: Boolean = true,
    val itens: List<RendaItemUi> = emptyList(),
    val totalMensalAtivoCentavos: Long = 0,
)

@HiltViewModel
class RendasViewModel @Inject constructor(
    rendaRepository: RendaRepository,
    configuracoesRepository: ConfiguracoesRepository,
    private val calculadora: CalculadoraRecebimento,
    private val clock: Clock,
) : ViewModel() {

    val uiState: StateFlow<RendasUiState> = combine(
        rendaRepository.observarRendas(),
        configuracoesRepository.anteciparDiaNaoUtil,
    ) { rendas, antecipar ->
        val hoje = LocalDate.now(clock)
        val ordenadas = rendas.sortedWith(
            compareByDescending<Renda> { it.ativo }
                .thenBy { it.tipo.ordinal }
                .thenBy { it.descricao.lowercase() },
        )
        RendasUiState(
            carregando = false,
            itens = ordenadas.map { RendaItemUi(it, calculadora.proximoRecebimento(it, hoje, antecipar)) },
            totalMensalAtivoCentavos = rendas
                .filter { it.ativo && (it.dataFim == null || !it.dataFim.isBefore(hoje)) }
                .sumOf { it.valorCentavos },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RendasUiState())
}
