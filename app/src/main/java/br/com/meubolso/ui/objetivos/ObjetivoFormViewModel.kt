package br.com.meubolso.ui.objetivos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.meubolso.data.repository.ObjetivoRepository
import br.com.meubolso.domain.model.Objetivo
import br.com.meubolso.domain.objetivo.CampoObjetivo
import br.com.meubolso.domain.objetivo.ValidadorObjetivo
import br.com.meubolso.domain.validacao.ErroValidacao
import br.com.meubolso.ui.navigation.Rotas
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class ObjetivoFormUiState(
    val id: Long? = null,
    val nome: String = "",
    val valorAlvoCentavos: Long = 0,
    val dataAlvo: LocalDate? = null,
    val erros: Map<CampoObjetivo, ErroValidacao> = emptyMap(),
    val confirmarExclusao: Boolean = false,
    val concluido: Boolean = false,
) {
    val editando: Boolean get() = id != null
}

@HiltViewModel
class ObjetivoFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ObjetivoRepository,
) : ViewModel() {

    private val idObjetivo: Long? = savedStateHandle.get<Long>(Rotas.ARG_ID)?.takeIf { it > 0 }

    private val _uiState = MutableStateFlow(ObjetivoFormUiState(id = idObjetivo))
    val uiState: StateFlow<ObjetivoFormUiState> = _uiState.asStateFlow()

    init {
        if (idObjetivo != null) {
            viewModelScope.launch {
                repository.buscar(idObjetivo)?.let { objetivo ->
                    _uiState.update {
                        it.copy(nome = objetivo.nome, valorAlvoCentavos = objetivo.valorAlvoCentavos, dataAlvo = objetivo.dataAlvo)
                    }
                }
            }
        }
    }

    fun alterarNome(valor: String) = atualizar(CampoObjetivo.NOME) { copy(nome = valor) }

    fun alterarValorAlvo(centavos: Long) = atualizar(CampoObjetivo.VALOR_ALVO) { copy(valorAlvoCentavos = centavos) }

    fun alterarDataAlvo(data: LocalDate?) = atualizar { copy(dataAlvo = data) }

    fun salvar() {
        val estado = _uiState.value
        val erros = ValidadorObjetivo.validar(estado.nome, estado.valorAlvoCentavos)
        if (erros.isNotEmpty()) {
            _uiState.update { it.copy(erros = erros) }
            return
        }
        val objetivo = Objetivo(
            id = estado.id ?: 0,
            nome = estado.nome.trim(),
            valorAlvoCentavos = estado.valorAlvoCentavos,
            dataAlvo = estado.dataAlvo,
        )
        viewModelScope.launch {
            repository.salvar(objetivo)
            _uiState.update { it.copy(concluido = true) }
        }
    }

    fun pedirExclusao() = _uiState.update { it.copy(confirmarExclusao = true) }

    fun cancelarExclusao() = _uiState.update { it.copy(confirmarExclusao = false) }

    fun excluir() {
        val id = idObjetivo ?: return
        viewModelScope.launch {
            repository.excluir(id)
            _uiState.update { it.copy(confirmarExclusao = false, concluido = true) }
        }
    }

    /** Aplica a alteração e limpa o erro dos campos afetados. */
    private fun atualizar(vararg campos: CampoObjetivo, alteracao: ObjetivoFormUiState.() -> ObjetivoFormUiState) {
        _uiState.update { it.alteracao().copy(erros = it.erros - campos.toSet()) }
    }
}
