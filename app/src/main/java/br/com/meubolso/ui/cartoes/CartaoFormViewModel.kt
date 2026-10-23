package br.com.meubolso.ui.cartoes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.meubolso.data.repository.CartaoRepository
import br.com.meubolso.domain.cartao.CampoCartao
import br.com.meubolso.domain.cartao.ValidadorCartao
import br.com.meubolso.domain.model.Cartao
import br.com.meubolso.domain.model.Origem
import br.com.meubolso.domain.validacao.ErroValidacao
import br.com.meubolso.ui.navigation.Rotas
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CartaoFormUiState(
    val id: Long? = null,
    val nome: String = "",
    val diaFechamento: String = "",
    val diaVencimento: String = "",
    val fonte: Origem? = null,
    val erros: Map<CampoCartao, ErroValidacao> = emptyMap(),
    /** Despesas lançadas no cartão; preenchido ao pedir a exclusão (null = sem diálogo aberto). */
    val confirmarExclusao: Int? = null,
    val concluido: Boolean = false,
) {
    val editando: Boolean get() = id != null
}

@HiltViewModel
class CartaoFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: CartaoRepository,
) : ViewModel() {

    private val idCartao: Long? = savedStateHandle.get<Long>(Rotas.ARG_ID)?.takeIf { it > 0 }

    private val _uiState = MutableStateFlow(CartaoFormUiState(id = idCartao))
    val uiState: StateFlow<CartaoFormUiState> = _uiState.asStateFlow()

    init {
        if (idCartao != null) {
            viewModelScope.launch {
                repository.buscar(idCartao)?.let { cartao ->
                    _uiState.update {
                        it.copy(
                            nome = cartao.nome,
                            diaFechamento = cartao.diaFechamento.toString(),
                            diaVencimento = cartao.diaVencimento.toString(),
                            fonte = cartao.fonte,
                        )
                    }
                }
            }
        }
    }

    fun alterarNome(valor: String) = atualizar(CampoCartao.NOME) { copy(nome = valor) }

    fun alterarDiaFechamento(valor: String) =
        atualizar(CampoCartao.FECHAMENTO) { copy(diaFechamento = valor.filter(Char::isDigit).take(2)) }

    fun alterarDiaVencimento(valor: String) =
        atualizar(CampoCartao.VENCIMENTO) { copy(diaVencimento = valor.filter(Char::isDigit).take(2)) }

    fun alterarFonte(valor: Origem) = atualizar(CampoCartao.FONTE) { copy(fonte = valor) }

    fun salvar() {
        val estado = _uiState.value
        val erros = ValidadorCartao.validar(
            nome = estado.nome,
            diaFechamento = estado.diaFechamento.toIntOrNull(),
            diaVencimento = estado.diaVencimento.toIntOrNull(),
            fonte = estado.fonte,
        )
        if (erros.isNotEmpty()) {
            _uiState.update { it.copy(erros = erros) }
            return
        }
        val cartao = Cartao(
            id = estado.id ?: 0,
            nome = estado.nome.trim(),
            diaFechamento = checkNotNull(estado.diaFechamento.toIntOrNull()),
            diaVencimento = checkNotNull(estado.diaVencimento.toIntOrNull()),
            fonte = checkNotNull(estado.fonte),
        )
        viewModelScope.launch {
            repository.salvar(cartao)
            _uiState.update { it.copy(concluido = true) }
        }
    }

    /** Conta as despesas antes de abrir a confirmação, para avisar o que será apagado junto. */
    fun pedirExclusao() {
        val id = idCartao ?: return
        viewModelScope.launch {
            val compras = repository.contarCompras(id)
            _uiState.update { it.copy(confirmarExclusao = compras) }
        }
    }

    fun cancelarExclusao() = _uiState.update { it.copy(confirmarExclusao = null) }

    fun excluir() {
        val id = idCartao ?: return
        viewModelScope.launch {
            repository.excluir(id)
            _uiState.update { it.copy(confirmarExclusao = null, concluido = true) }
        }
    }

    /** Aplica a alteração e limpa o erro dos campos afetados. */
    private fun atualizar(vararg campos: CampoCartao, alteracao: CartaoFormUiState.() -> CartaoFormUiState) {
        _uiState.update { it.alteracao().copy(erros = it.erros - campos.toSet()) }
    }
}
