package br.com.meubolso.ui.configuracoes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.meubolso.data.repository.ConfiguracoesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConfiguracoesViewModel @Inject constructor(
    private val repository: ConfiguracoesRepository,
) : ViewModel() {

    val anteciparDiaNaoUtil: StateFlow<Boolean> = repository.anteciparDiaNaoUtil
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun alterarAnteciparDiaNaoUtil(ativo: Boolean) {
        viewModelScope.launch { repository.definirAnteciparDiaNaoUtil(ativo) }
    }
}
