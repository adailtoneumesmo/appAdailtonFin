package br.com.meubolso.ui.rendas

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.meubolso.data.repository.ConfiguracoesRepository
import br.com.meubolso.data.repository.RendaRepository
import br.com.meubolso.domain.model.ModoDiaRecebimento
import br.com.meubolso.domain.model.Renda
import br.com.meubolso.domain.model.TipoRenda
import br.com.meubolso.domain.renda.CalculadoraRecebimento
import br.com.meubolso.domain.renda.CampoRenda
import br.com.meubolso.domain.renda.DataRecebimento
import br.com.meubolso.domain.renda.ValidadorRenda
import br.com.meubolso.domain.validacao.ErroValidacao
import br.com.meubolso.ui.navigation.Rotas
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

data class RendaFormUiState(
    val id: Long? = null,
    val descricao: String = "",
    val tipo: TipoRenda = TipoRenda.SALARIO,
    val valorCentavos: Long = 0,
    val modoDia: ModoDiaRecebimento = ModoDiaRecebimento.DIA_FIXO,
    val dia: String = "",
    val ativo: Boolean = true,
    val dataInicio: LocalDate? = null,
    val dataFim: LocalDate? = null,
    val erros: Map<CampoRenda, ErroValidacao> = emptyMap(),
    val concluido: Boolean = false,
) {
    val editando: Boolean get() = id != null
}

@HiltViewModel
class RendaFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val rendaRepository: RendaRepository,
    configuracoesRepository: ConfiguracoesRepository,
    private val calculadora: CalculadoraRecebimento,
    private val clock: Clock,
) : ViewModel() {

    private val idRenda: Long? = savedStateHandle.get<Long>(Rotas.ARG_ID)?.takeIf { it > 0 }

    private val _uiState = MutableStateFlow(RendaFormUiState(id = idRenda, dataInicio = LocalDate.now(clock)))
    val uiState: StateFlow<RendaFormUiState> = _uiState.asStateFlow()

    /** Próximo recebimento conforme o que está preenchido (null enquanto o formulário estiver incompleto). */
    val previsao: StateFlow<DataRecebimento?> = combine(
        _uiState,
        configuracoesRepository.anteciparDiaNaoUtil,
    ) { estado, antecipar ->
        estado.paraRendaOuNull()?.let { calculadora.proximoRecebimento(it, LocalDate.now(clock), antecipar) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        if (idRenda != null) {
            viewModelScope.launch {
                rendaRepository.buscar(idRenda)?.let { renda ->
                    _uiState.update {
                        it.copy(
                            descricao = renda.descricao,
                            tipo = renda.tipo,
                            valorCentavos = renda.valorCentavos,
                            modoDia = renda.modoDia,
                            dia = renda.dia.toString(),
                            ativo = renda.ativo,
                            dataInicio = renda.dataInicio,
                            dataFim = renda.dataFim,
                        )
                    }
                }
            }
        }
    }

    fun alterarDescricao(valor: String) = atualizar(CampoRenda.DESCRICAO) { copy(descricao = valor) }

    fun alterarTipo(valor: TipoRenda) = _uiState.update { it.copy(tipo = valor) }

    fun alterarValor(centavos: Long) = atualizar(CampoRenda.VALOR) { copy(valorCentavos = centavos) }

    fun alterarModoDia(valor: ModoDiaRecebimento) = atualizar(CampoRenda.DIA) { copy(modoDia = valor) }

    fun alterarDia(valor: String) =
        atualizar(CampoRenda.DIA) { copy(dia = valor.filter(Char::isDigit).take(2)) }

    fun alterarAtivo(valor: Boolean) = _uiState.update { it.copy(ativo = valor) }

    fun alterarDataInicio(valor: LocalDate?) =
        atualizar(CampoRenda.DATA_INICIO, CampoRenda.DATA_FIM) { copy(dataInicio = valor) }

    fun alterarDataFim(valor: LocalDate?) = atualizar(CampoRenda.DATA_FIM) { copy(dataFim = valor) }

    fun salvar() {
        val estado = _uiState.value
        val erros = ValidadorRenda.validar(
            descricao = estado.descricao,
            valorCentavos = estado.valorCentavos,
            modoDia = estado.modoDia,
            dia = estado.dia.toIntOrNull(),
            dataInicio = estado.dataInicio,
            dataFim = estado.dataFim,
        )
        if (erros.isNotEmpty()) {
            _uiState.update { it.copy(erros = erros) }
            return
        }
        val renda = checkNotNull(estado.paraRendaOuNull())
        viewModelScope.launch {
            rendaRepository.salvar(renda)
            _uiState.update { it.copy(concluido = true) }
        }
    }

    fun excluir() {
        val id = idRenda ?: return
        viewModelScope.launch {
            rendaRepository.excluir(id)
            _uiState.update { it.copy(concluido = true) }
        }
    }

    /** Aplica a alteração e limpa o erro dos campos afetados. */
    private fun atualizar(vararg campos: CampoRenda, alteracao: RendaFormUiState.() -> RendaFormUiState) {
        _uiState.update { it.alteracao().copy(erros = it.erros - campos.toSet()) }
    }

    private fun RendaFormUiState.paraRendaOuNull(): Renda? {
        val diaInt = dia.toIntOrNull() ?: return null
        val limite = if (modoDia == ModoDiaRecebimento.DIA_UTIL) ValidadorRenda.MAX_DIA_UTIL else 31
        if (diaInt !in 1..limite) return null
        return Renda(
            id = id ?: 0,
            descricao = descricao.trim(),
            tipo = tipo,
            valorCentavos = valorCentavos,
            modoDia = modoDia,
            dia = diaInt,
            ativo = ativo,
            dataInicio = dataInicio ?: return null,
            dataFim = dataFim,
        )
    }
}
