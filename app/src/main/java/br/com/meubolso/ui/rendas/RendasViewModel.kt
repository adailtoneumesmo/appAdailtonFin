package br.com.meubolso.ui.rendas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.meubolso.data.repository.ConfiguracoesRepository
import br.com.meubolso.data.repository.DespesaRepository
import br.com.meubolso.data.repository.RendaRepository
import br.com.meubolso.domain.model.Renda
import br.com.meubolso.domain.model.TipoRenda
import br.com.meubolso.domain.renda.CalculadoraRecebimento
import br.com.meubolso.domain.renda.DataRecebimento
import br.com.meubolso.domain.vale.CalculadoraValeAlimentacao
import br.com.meubolso.domain.vale.SaldoVale
import br.com.meubolso.domain.vale.ValorVale
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class RendaItemUi(
    val renda: Renda,
    val proximoRecebimento: DataRecebimento?,
    /** Só para VR/VA: valor no mês atual (no cálculo por dia, depende dos dias úteis). */
    val valorVale: ValorVale? = null,
)

data class RendasUiState(
    val carregando: Boolean = true,
    val itens: List<RendaItemUi> = emptyList(),
    /** Salário, vale e renda extra; o VR/VA fica em [saldoVale]. */
    val totalMensalAtivoCentavos: Long = 0,
    val mes: YearMonth? = null,
    /** `null` quando não há VR/VA cadastrado. */
    val saldoVale: SaldoVale? = null,
)

@HiltViewModel
class RendasViewModel @Inject constructor(
    rendaRepository: RendaRepository,
    despesaRepository: DespesaRepository,
    configuracoesRepository: ConfiguracoesRepository,
    private val calculadora: CalculadoraRecebimento,
    private val calculadoraVale: CalculadoraValeAlimentacao,
    private val clock: Clock,
) : ViewModel() {

    private val mesAtual = YearMonth.now(clock)

    val uiState: StateFlow<RendasUiState> = combine(
        rendaRepository.observarRendas(),
        configuracoesRepository.anteciparDiaNaoUtil,
        despesaRepository.observarDespesas(),
        despesaRepository.observarPagamentos(mesAtual),
    ) { rendas, antecipar, despesas, pagamentos ->
        val hoje = LocalDate.now(clock)
        val possuiVale = rendas.any { it.tipo == TipoRenda.VALE_ALIMENTACAO }
        val ordenadas = rendas.sortedWith(
            compareByDescending<Renda> { it.ativo }
                .thenBy { it.tipo.ordinal }
                .thenBy { it.descricao.lowercase() },
        )
        RendasUiState(
            carregando = false,
            itens = ordenadas.map { renda ->
                val vale = renda.tipo == TipoRenda.VALE_ALIMENTACAO
                RendaItemUi(
                    renda = renda,
                    // O VR/VA é creditado no dia informado, sem antecipação.
                    proximoRecebimento = calculadora.proximoRecebimento(renda, hoje, antecipar && !vale),
                    valorVale = if (vale) calculadoraVale.valorNoMes(renda, mesAtual) else null,
                )
            },
            totalMensalAtivoCentavos = rendas
                .filter { it.tipo != TipoRenda.VALE_ALIMENTACAO }
                .filter { it.ativo && (it.dataFim == null || !it.dataFim.isBefore(hoje)) }
                .sumOf { it.valorCentavos },
            mes = mesAtual,
            saldoVale = if (possuiVale) calculadoraVale.saldoNoMes(rendas, despesas, pagamentos, mesAtual) else null,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RendasUiState())
}
