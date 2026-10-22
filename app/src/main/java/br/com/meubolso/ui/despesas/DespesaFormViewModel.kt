package br.com.meubolso.ui.despesas

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.meubolso.data.repository.CategoriaRepository
import br.com.meubolso.data.repository.DespesaRepository
import br.com.meubolso.domain.despesa.CampoDespesa
import br.com.meubolso.domain.despesa.ValidadorDespesa
import br.com.meubolso.domain.model.Categoria
import br.com.meubolso.domain.model.DespesaFixa
import br.com.meubolso.domain.model.FormaPagamento
import br.com.meubolso.domain.model.Recorrencia
import br.com.meubolso.domain.model.VinculoPeriodo
import br.com.meubolso.domain.validacao.ErroValidacao
import br.com.meubolso.ui.navigation.Rotas
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.YearMonth
import javax.inject.Inject

data class DespesaFormUiState(
    val id: Long? = null,
    val descricao: String = "",
    val categoriaId: Long? = null,
    val valorCentavos: Long = 0,
    val dia: String = "",
    val formaPagamento: FormaPagamento = FormaPagamento.PIX,
    val vinculo: VinculoPeriodo = VinculoPeriodo.AUTOMATICO,
    val recorrencia: Recorrencia = Recorrencia.MENSAL,
    val totalParcelas: String = "",
    val mesInicio: YearMonth,
    val ativa: Boolean = true,
    val erros: Map<CampoDespesa, ErroValidacao> = emptyMap(),
    val concluido: Boolean = false,
) {
    val editando: Boolean get() = id != null
}

@HiltViewModel
class DespesaFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val despesaRepository: DespesaRepository,
    categoriaRepository: CategoriaRepository,
    clock: Clock,
) : ViewModel() {

    private val idDespesa: Long? = savedStateHandle.get<Long>(Rotas.ARG_ID)?.takeIf { it > 0 }

    private val _uiState = MutableStateFlow(DespesaFormUiState(id = idDespesa, mesInicio = YearMonth.now(clock)))
    val uiState: StateFlow<DespesaFormUiState> = _uiState.asStateFlow()

    val categorias: StateFlow<List<Categoria>> = categoriaRepository.observarCategorias()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        if (idDespesa != null) {
            viewModelScope.launch {
                despesaRepository.buscar(idDespesa)?.let { despesa ->
                    _uiState.update {
                        it.copy(
                            descricao = despesa.descricao,
                            categoriaId = despesa.categoriaId,
                            valorCentavos = despesa.valorCentavos,
                            dia = despesa.diaVencimento.toString(),
                            formaPagamento = despesa.formaPagamento,
                            vinculo = despesa.vinculo,
                            recorrencia = despesa.recorrencia,
                            totalParcelas = despesa.totalParcelas?.toString().orEmpty(),
                            mesInicio = despesa.mesInicio,
                            ativa = despesa.ativa,
                        )
                    }
                }
            }
        }
    }

    fun alterarDescricao(valor: String) = atualizar(CampoDespesa.DESCRICAO) { copy(descricao = valor) }

    fun alterarCategoria(categoria: Categoria) = atualizar(CampoDespesa.CATEGORIA) { copy(categoriaId = categoria.id) }

    fun alterarValor(centavos: Long) = atualizar(CampoDespesa.VALOR) { copy(valorCentavos = centavos) }

    fun alterarDia(valor: String) = atualizar(CampoDespesa.DIA) { copy(dia = valor.filter(Char::isDigit).take(2)) }

    fun alterarFormaPagamento(valor: FormaPagamento) = _uiState.update { it.copy(formaPagamento = valor) }

    fun alterarVinculo(valor: VinculoPeriodo) = _uiState.update { it.copy(vinculo = valor) }

    fun alterarRecorrencia(valor: Recorrencia) = atualizar(CampoDespesa.PARCELAS) { copy(recorrencia = valor) }

    fun alterarTotalParcelas(valor: String) =
        atualizar(CampoDespesa.PARCELAS) { copy(totalParcelas = valor.filter(Char::isDigit).take(3)) }

    fun alterarMesInicio(valor: YearMonth) = _uiState.update { it.copy(mesInicio = valor) }

    fun alterarAtiva(valor: Boolean) = _uiState.update { it.copy(ativa = valor) }

    fun salvar() {
        val estado = _uiState.value
        val parcelas = estado.totalParcelas.toIntOrNull()
        val erros = ValidadorDespesa.validar(
            descricao = estado.descricao,
            categoriaId = estado.categoriaId,
            valorCentavos = estado.valorCentavos,
            diaVencimento = estado.dia.toIntOrNull(),
            recorrencia = estado.recorrencia,
            totalParcelas = parcelas,
        )
        if (erros.isNotEmpty()) {
            _uiState.update { it.copy(erros = erros) }
            return
        }
        val despesa = DespesaFixa(
            id = estado.id ?: 0,
            descricao = estado.descricao.trim(),
            categoriaId = checkNotNull(estado.categoriaId),
            valorCentavos = estado.valorCentavos,
            diaVencimento = checkNotNull(estado.dia.toIntOrNull()),
            formaPagamento = estado.formaPagamento,
            vinculo = estado.vinculo,
            recorrencia = estado.recorrencia,
            totalParcelas = if (estado.recorrencia == Recorrencia.PARCELADA) parcelas else null,
            mesInicio = estado.mesInicio,
            ativa = estado.ativa,
        )
        viewModelScope.launch {
            despesaRepository.salvar(despesa)
            _uiState.update { it.copy(concluido = true) }
        }
    }

    fun excluir() {
        val id = idDespesa ?: return
        viewModelScope.launch {
            despesaRepository.excluir(id)
            _uiState.update { it.copy(concluido = true) }
        }
    }

    /** Aplica a alteração e limpa o erro dos campos afetados. */
    private fun atualizar(vararg campos: CampoDespesa, alteracao: DespesaFormUiState.() -> DespesaFormUiState) {
        _uiState.update { it.alteracao().copy(erros = it.erros - campos.toSet()) }
    }
}
