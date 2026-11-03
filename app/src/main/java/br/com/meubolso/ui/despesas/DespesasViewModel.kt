package br.com.meubolso.ui.despesas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.meubolso.data.repository.CartaoRepository
import br.com.meubolso.data.repository.CategoriaRepository
import br.com.meubolso.data.repository.ConfiguracoesRepository
import br.com.meubolso.data.repository.DespesaRepository
import br.com.meubolso.data.repository.RendaRepository
import br.com.meubolso.domain.dashboard.CalculadoraDashboard
import br.com.meubolso.domain.dashboard.DetalheOrigem
import br.com.meubolso.domain.model.Cartao
import br.com.meubolso.domain.model.Categoria
import br.com.meubolso.domain.model.CompraCartao
import br.com.meubolso.domain.model.DespesaFixa
import br.com.meubolso.domain.model.Fatura
import br.com.meubolso.domain.model.OcorrenciaDespesa
import br.com.meubolso.domain.model.Recorrencia
import br.com.meubolso.domain.model.Renda
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

data class FaturaUi(
    val fatura: Fatura,
    val dataPagamento: LocalDate?,
    val vencida: Boolean,
) {
    val paga: Boolean get() = dataPagamento != null
}

/**
 * Renda de um período (salário ou vale) contra as despesas e faturas pagas com ela —
 * os mesmos valores do Início.
 */
data class PeriodoDespesasUi(
    val rendaCentavos: Long = 0,
    val ocorrencias: List<OcorrenciaUi> = emptyList(),
    val faturas: List<FaturaUi> = emptyList(),
) {
    val totalCentavos: Long
        get() = ocorrencias.sumOf { it.ocorrencia.despesa.valorCentavos } + faturas.sumOf { it.fatura.totalCentavos }

    val pagoCentavos: Long
        get() = pagas.sumOf { it.ocorrencia.despesa.valorCentavos } + faturasPagas.sumOf { it.fatura.totalCentavos }

    val pendenteCentavos: Long get() = totalCentavos - pagoCentavos

    val subtotalCentavos: Long get() = rendaCentavos - totalCentavos

    /** Ocorrências ainda em aberto — ficam visíveis logo abaixo do resumo. */
    val pendentes: List<OcorrenciaUi> get() = ocorrencias.filterNot { it.ocorrencia.paga }

    /** Ocorrências já pagas, arquivadas numa seção recolhível. */
    val pagas: List<OcorrenciaUi> get() = ocorrencias.filter { it.ocorrencia.paga }

    val faturasPendentes: List<FaturaUi> get() = faturas.filterNot { it.paga }

    val faturasPagas: List<FaturaUi> get() = faturas.filter { it.paga }

    val tudoPago: Boolean get() = pendentes.isEmpty() && faturasPendentes.isEmpty()

    val quantidadePagas: Int get() = pagas.size + faturasPagas.size

    val semLancamentos: Boolean get() = ocorrencias.isEmpty() && faturas.isEmpty()

    val vazio: Boolean get() = rendaCentavos == 0L && semLancamentos
}

data class DespesasUiState(
    val carregando: Boolean = true,
    val mes: YearMonth,
    val possuiDespesas: Boolean = false,
    val salario: PeriodoDespesasUi = PeriodoDespesasUi(),
    val vale: PeriodoDespesasUi = PeriodoDespesasUi(),
    val semCobranca: List<DespesaSemCobrancaUi> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DespesasViewModel @Inject constructor(
    private val despesaRepository: DespesaRepository,
    rendaRepository: RendaRepository,
    private val cartaoRepository: CartaoRepository,
    categoriaRepository: CategoriaRepository,
    configuracoesRepository: ConfiguracoesRepository,
    private val calculadora: CalculadoraDashboard,
    private val clock: Clock,
) : ViewModel() {

    private val mes = MutableStateFlow(YearMonth.now(clock))

    val uiState: StateFlow<DespesasUiState> = combine(
        mes,
        // combine aceita no máximo 5 Flows tipados; os demais vão agrupados.
        combine(
            rendaRepository.observarRendas(),
            configuracoesRepository.anteciparDiaNaoUtil,
            cartaoRepository.observarCartoes(),
            cartaoRepository.observarCompras(),
            ::Complementos,
        ),
        despesaRepository.observarDespesas(),
        categoriaRepository.observarCategorias(),
        mes.flatMapLatest { atual ->
            // A fatura que vence no mês pode ser do mês de referência anterior.
            combine(
                despesaRepository.observarPagamentos(atual),
                cartaoRepository.observarPagamentosFatura(atual.minusMonths(1), atual),
                ::Pair,
            )
        },
    ) { mes, (rendas, antecipar, cartoes, compras), todasDespesas, categorias, (pagamentos, pagamentosFatura) ->
        val hoje = LocalDate.now(clock)
        val categoriasPorId = categorias.associateBy { it.id }
        // O VR/VA tem controle próprio e não aparece nesta tela.
        val despesas = todasDespesas.filterNot { it.pagaComValeAlimentacao }
        // Mesma divisão Salário x Vale do Início, faturas incluídas.
        val resumo = calculadora.resumir(rendas, despesas, pagamentos, mes, antecipar, cartoes, compras)
        val pagamentoDaFatura = pagamentosFatura.associate { (it.cartaoId to it.mes) to it.dataPagamento }

        fun periodo(renda: Long, detalhe: DetalheOrigem) = PeriodoDespesasUi(
            rendaCentavos = renda,
            ocorrencias = detalhe.despesas
                .sortedBy { it.vencimento }
                .map { OcorrenciaUi(it, categoriasPorId[it.despesa.categoriaId], it.vencida(hoje)) },
            faturas = detalhe.faturas.map { fatura ->
                val dataPagamento = pagamentoDaFatura[fatura.cartao.id to fatura.mes]
                FaturaUi(fatura, dataPagamento, vencida = dataPagamento == null && fatura.vencimento.isBefore(hoje))
            },
        )

        val idsComCobranca = (resumo.detalheSalario.despesas + resumo.detalheVale.despesas)
            .map { it.despesa.id }
            .toSet()
        DespesasUiState(
            carregando = false,
            mes = mes,
            possuiDespesas = despesas.isNotEmpty() || compras.isNotEmpty(),
            salario = periodo(resumo.salario.rendaCentavos, resumo.detalheSalario),
            vale = periodo(resumo.vale.rendaCentavos, resumo.detalheVale),
            // Despesas únicas (ex.: compras de mercado) só aparecem no próprio mês.
            semCobranca = despesas
                .filter { it.id !in idsComCobranca && it.recorrencia != Recorrencia.UNICA }
                .map { despesa ->
                    val motivo = when {
                        !despesa.ativa -> MotivoSemCobranca.INATIVA
                        mes < despesa.mesInicio -> MotivoSemCobranca.NAO_INICIADA
                        else -> MotivoSemCobranca.ENCERRADA
                    }
                    DespesaSemCobrancaUi(despesa, categoriasPorId[despesa.categoriaId], motivo)
                },
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

    /** Alterna PAGA/PENDENTE da fatura; ao marcar como paga, grava a data de hoje. */
    fun alternarPagamentoFatura(item: FaturaUi) {
        viewModelScope.launch {
            val fatura = item.fatura
            if (item.paga) {
                cartaoRepository.marcarFaturaComoPendente(fatura.cartao.id, fatura.mes)
            } else {
                cartaoRepository.marcarFaturaComoPaga(fatura.cartao.id, fatura.mes, LocalDate.now(clock))
            }
        }
    }

    private data class Complementos(
        val rendas: List<Renda>,
        val anteciparDiaNaoUtil: Boolean,
        val cartoes: List<Cartao>,
        val compras: List<CompraCartao>,
    )
}
