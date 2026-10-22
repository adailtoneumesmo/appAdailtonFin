package br.com.meubolso.ui.despesas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.meubolso.data.repository.CategoriaRepository
import br.com.meubolso.data.repository.DespesaRepository
import br.com.meubolso.domain.despesa.CalculadoraOcorrencias
import br.com.meubolso.domain.model.Categoria
import br.com.meubolso.domain.model.DespesaFixa
import br.com.meubolso.domain.model.OcorrenciaDespesa
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class OcorrenciaUi(
    val ocorrencia: OcorrenciaDespesa,
    val categoria: Categoria?,
    val vencida: Boolean,
)

enum class MotivoSemCobranca { INATIVA, ENCERRADA, NAO_INICIADA }

data class DespesaSemCobrancaUi(
    val despesa: DespesaFixa,
    val categoria: Categoria?,
    val motivo: MotivoSemCobranca,
)

data class DespesasUiState(
    val carregando: Boolean = true,
    val mes: YearMonth,
    val possuiDespesas: Boolean = false,
    val ocorrencias: List<OcorrenciaUi> = emptyList(),
    val semCobranca: List<DespesaSemCobrancaUi> = emptyList(),
    val totalCentavos: Long = 0,
    val pagoCentavos: Long = 0,
) {
    val pendenteCentavos: Long get() = totalCentavos - pagoCentavos
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DespesasViewModel @Inject constructor(
    private val despesaRepository: DespesaRepository,
    categoriaRepository: CategoriaRepository,
    private val calculadora: CalculadoraOcorrencias,
    private val clock: Clock,
) : ViewModel() {

    private val mes = MutableStateFlow(YearMonth.now(clock))

    val uiState: StateFlow<DespesasUiState> = combine(
        mes,
        despesaRepository.observarDespesas(),
        categoriaRepository.observarCategorias(),
        mes.flatMapLatest { despesaRepository.observarPagamentos(it) },
    ) { mes, despesas, categorias, pagamentos ->
        val hoje = LocalDate.now(clock)
        val categoriasPorId = categorias.associateBy { it.id }
        val ocorrencias = calculadora.ocorrenciasNoMes(despesas, pagamentos, mes)
        val idsComCobranca = ocorrencias.map { it.despesa.id }.toSet()
        DespesasUiState(
            carregando = false,
            mes = mes,
            possuiDespesas = despesas.isNotEmpty(),
            ocorrencias = ocorrencias.map {
                OcorrenciaUi(it, categoriasPorId[it.despesa.categoriaId], it.vencida(hoje))
            },
            semCobranca = despesas
                .filter { it.id !in idsComCobranca }
                .map { despesa ->
                    val motivo = when {
                        !despesa.ativa -> MotivoSemCobranca.INATIVA
                        mes < despesa.mesInicio -> MotivoSemCobranca.NAO_INICIADA
                        else -> MotivoSemCobranca.ENCERRADA
                    }
                    DespesaSemCobrancaUi(despesa, categoriasPorId[despesa.categoriaId], motivo)
                },
            totalCentavos = ocorrencias.sumOf { it.despesa.valorCentavos },
            pagoCentavos = ocorrencias.filter { it.paga }.sumOf { it.despesa.valorCentavos },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DespesasUiState(mes = mes.value))

    fun alterarMes(novo: YearMonth) {
        mes.value = novo
    }

    /** Alterna PAGA/PENDENTE; ao marcar como paga, grava a data de hoje. */
    fun alternarPagamento(ocorrencia: OcorrenciaDespesa) {
        viewModelScope.launch {
            if (ocorrencia.paga) {
                despesaRepository.marcarComoPendente(ocorrencia.despesa.id, ocorrencia.competencia)
            } else {
                despesaRepository.marcarComoPaga(ocorrencia.despesa.id, ocorrencia.competencia, LocalDate.now(clock))
            }
        }
    }
}
