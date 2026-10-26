package br.com.meubolso.ui.conta

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.meubolso.R
import br.com.meubolso.data.remoto.ContaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OrcamentoUiState(
    val nome: String = "",
    val codigo: String = "",
    val carregando: Boolean = false,
    @StringRes val erro: Int? = null,
)

@HiltViewModel
class OrcamentoViewModel @Inject constructor(
    private val conta: ContaRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrcamentoUiState())
    val uiState: StateFlow<OrcamentoUiState> = _uiState.asStateFlow()

    fun alterarNome(valor: String) = _uiState.update { it.copy(nome = valor, erro = null) }

    fun alterarCodigo(valor: String) =
        _uiState.update { it.copy(codigo = valor.filter(Char::isLetterOrDigit).take(6).uppercase(), erro = null) }

    fun criar() {
        val nome = _uiState.value.nome
        if (nome.isBlank()) {
            _uiState.update { it.copy(erro = R.string.orcamento_erro_nome) }
            return
        }
        executar { conta.criarOrcamento(nome) }
    }

    fun entrar() {
        val codigo = _uiState.value.codigo
        if (codigo.length != 6) {
            _uiState.update { it.copy(erro = R.string.conta_erro_codigo_invalido) }
            return
        }
        executar { conta.entrarComCodigo(codigo) }
    }

    fun sair() {
        viewModelScope.launch { runCatching { conta.sair() } }
    }

    private fun executar(acao: suspend () -> Unit) {
        _uiState.update { it.copy(carregando = true, erro = null) }
        viewModelScope.launch {
            val resultado = runCatching { acao() }
            _uiState.update { it.copy(carregando = false, erro = resultado.exceptionOrNull()?.let(::mensagemErroConta)) }
        }
    }
}
