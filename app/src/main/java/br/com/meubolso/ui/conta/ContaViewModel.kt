package br.com.meubolso.ui.conta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.meubolso.data.remoto.AlteracoesNaoEnviadasException
import br.com.meubolso.data.remoto.ContaRepository
import br.com.meubolso.data.remoto.EstadoSessao
import br.com.meubolso.data.remoto.EstadoSincronizacao
import br.com.meubolso.data.remoto.GerenciadorSincronizacao
import br.com.meubolso.data.remoto.Orcamento
import br.com.meubolso.data.remoto.Usuario
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContaUiState(
    val usuario: Usuario? = null,
    val orcamento: Orcamento? = null,
    val sincronizacao: EstadoSincronizacao = EstadoSincronizacao(),
    val pendentes: Int = 0,
    /** Sair falhou por haver alterações não enviadas; pede confirmação para perdê-las. */
    val confirmarSaida: Boolean = false,
    val saindo: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ContaViewModel @Inject constructor(
    private val conta: ContaRepository,
    private val gerenciador: GerenciadorSincronizacao,
) : ViewModel() {

    private val saida = MutableStateFlow(ContaUiState())

    val uiState: StateFlow<ContaUiState> = combine(
        conta.estado,
        conta.orcamentoAtualId.flatMapLatest { id -> id?.let(conta::observarOrcamento) ?: flowOf(null) },
        gerenciador.estado,
        gerenciador.quantidadePendentes,
        saida,
    ) { sessao, orcamento, sincronizacao, pendentes, saida ->
        saida.copy(
            usuario = (sessao as? EstadoSessao.Pronto)?.usuario,
            orcamento = orcamento,
            sincronizacao = sincronizacao,
            pendentes = pendentes,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ContaUiState())

    fun sincronizarAgora() {
        val id = (conta.estado.value as? EstadoSessao.Pronto)?.orcamentoId ?: return
        viewModelScope.launch { gerenciador.sincronizar(id) }
    }

    fun sair(forcar: Boolean = false) {
        saida.value = saida.value.copy(saindo = true, confirmarSaida = false)
        viewModelScope.launch {
            val resultado = runCatching { conta.sair(forcar) }
            saida.value = saida.value.copy(
                saindo = false,
                confirmarSaida = resultado.exceptionOrNull() is AlteracoesNaoEnviadasException,
            )
        }
    }

    fun cancelarSaida() {
        saida.value = saida.value.copy(confirmarSaida = false)
    }
}
