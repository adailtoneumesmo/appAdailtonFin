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

data class EntrarUiState(
    val criandoConta: Boolean = false,
    val nome: String = "",
    val email: String = "",
    val senha: String = "",
    val carregando: Boolean = false,
    @StringRes val erro: Int? = null,
    @StringRes val aviso: Int? = null,
)

@HiltViewModel
class EntrarViewModel @Inject constructor(
    private val conta: ContaRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EntrarUiState())
    val uiState: StateFlow<EntrarUiState> = _uiState.asStateFlow()

    fun alterarModo(criandoConta: Boolean) = _uiState.update { it.copy(criandoConta = criandoConta, erro = null, aviso = null) }

    fun alterarNome(valor: String) = _uiState.update { it.copy(nome = valor, erro = null) }

    fun alterarEmail(valor: String) = _uiState.update { it.copy(email = valor, erro = null) }

    fun alterarSenha(valor: String) = _uiState.update { it.copy(senha = valor, erro = null) }

    fun confirmar() {
        val estado = _uiState.value
        val erro = when {
            estado.criandoConta && estado.nome.isBlank() -> R.string.conta_erro_nome
            estado.email.isBlank() || '@' !in estado.email -> R.string.conta_erro_email
            estado.senha.length < TAMANHO_MINIMO_SENHA -> R.string.conta_erro_senha_fraca
            else -> null
        }
        if (erro != null) {
            _uiState.update { it.copy(erro = erro) }
            return
        }
        executar {
            if (estado.criandoConta) {
                conta.criarConta(estado.nome, estado.email, estado.senha)
            } else {
                conta.entrarComEmail(estado.email, estado.senha)
            }
        }
    }

    fun entrarComGoogle(idToken: String) = executar { conta.entrarComGoogle(idToken) }

    fun falhaGoogle() = _uiState.update { it.copy(erro = R.string.conta_erro_google) }

    fun redefinirSenha() {
        val email = _uiState.value.email
        if (email.isBlank() || '@' !in email) {
            _uiState.update { it.copy(erro = R.string.conta_erro_email) }
            return
        }
        executar {
            conta.redefinirSenha(email)
            _uiState.update { it.copy(aviso = R.string.conta_aviso_redefinicao) }
        }
    }

    /** Quando dá certo, a sessão muda e o app sai desta tela sozinho. */
    private fun executar(acao: suspend () -> Unit) {
        _uiState.update { it.copy(carregando = true, erro = null, aviso = null) }
        viewModelScope.launch {
            val resultado = runCatching { acao() }
            _uiState.update { it.copy(carregando = false, erro = resultado.exceptionOrNull()?.let(::mensagemErroConta)) }
        }
    }

    private companion object {
        /** Mínimo exigido pelo Firebase Auth. */
        const val TAMANHO_MINIMO_SENHA = 6
    }
}
