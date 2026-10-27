package br.com.meubolso.ui.objetivos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.meubolso.data.repository.ObjetivoRepository
import br.com.meubolso.domain.objetivo.CalculadoraObjetivos
import br.com.meubolso.domain.objetivo.ResumoObjetivos
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

data class ObjetivosUiState(
    val carregando: Boolean = true,
    val resumo: ResumoObjetivos = ResumoObjetivos(),
)

/** Os objetivos com o progresso de cada um; usado pela tela de objetivos e pelo card do Início. */
fun ObjetivoRepository.observarResumo(clock: Clock): Flow<ResumoObjetivos> =
    combine(observarObjetivos(), observarPosicaoAtual()) { objetivos, posicao ->
        CalculadoraObjetivos.resumir(objetivos, posicao, LocalDate.now(clock))
    }

@HiltViewModel
class ObjetivosViewModel @Inject constructor(
    private val repository: ObjetivoRepository,
    private val clock: Clock,
) : ViewModel() {

    val uiState: StateFlow<ObjetivosUiState> = repository.observarResumo(clock)
        .map { ObjetivosUiState(carregando = false, resumo = it) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ObjetivosUiState())

    /** Valor em edição no diálogo do patrimônio; null = diálogo fechado. */
    private val _edicaoPatrimonio = MutableStateFlow<Long?>(null)
    val edicaoPatrimonio: StateFlow<Long?> = _edicaoPatrimonio.asStateFlow()

    fun abrirEdicaoPatrimonio() {
        _edicaoPatrimonio.value = uiState.value.resumo.patrimonioCentavos
    }

    fun alterarPatrimonio(centavos: Long) = _edicaoPatrimonio.update { it?.let { centavos } }

    fun cancelarEdicaoPatrimonio() {
        _edicaoPatrimonio.value = null
    }

    fun salvarPatrimonio() {
        val valor = _edicaoPatrimonio.value ?: return
        viewModelScope.launch {
            repository.registrarPosicao(valor, LocalDate.now(clock))
            _edicaoPatrimonio.value = null
        }
    }
}
