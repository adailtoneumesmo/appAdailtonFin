package br.com.meubolso.ui.pendentes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.meubolso.data.repository.NotificacaoBancoRepository
import br.com.meubolso.domain.model.NotificacaoBanco
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PendentesUiState(
    val carregando: Boolean = true,
    val pendentes: List<NotificacaoBanco> = emptyList(),
)

@HiltViewModel
class PendentesViewModel @Inject constructor(
    private val repository: NotificacaoBancoRepository,
) : ViewModel() {

    val uiState: StateFlow<PendentesUiState> = repository.observarPendentes()
        .map { PendentesUiState(carregando = false, pendentes = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PendentesUiState())

    fun descartar(id: Long) {
        viewModelScope.launch { repository.descartar(id) }
    }
}
