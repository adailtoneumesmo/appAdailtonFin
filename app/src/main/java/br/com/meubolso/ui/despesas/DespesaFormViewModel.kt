package br.com.meubolso.ui.despesas

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.meubolso.data.repository.CartaoRepository
import br.com.meubolso.data.repository.CategoriaRepository
import br.com.meubolso.data.repository.DespesaRepository
import br.com.meubolso.data.repository.NotificacaoBancoRepository
import br.com.meubolso.data.repository.RendaRepository
import br.com.meubolso.domain.cartao.CalculadoraFatura
import br.com.meubolso.domain.cartao.ValidadorCompraCartao
import br.com.meubolso.domain.despesa.CampoDespesa
import br.com.meubolso.domain.despesa.ValidadorDespesa
import br.com.meubolso.domain.model.Cartao
import br.com.meubolso.domain.model.Categoria
import br.com.meubolso.domain.model.CompraCartao
import br.com.meubolso.domain.model.DespesaFixa
import br.com.meubolso.domain.model.FormaPagamento
import br.com.meubolso.domain.model.ModalidadeCompra
import br.com.meubolso.domain.model.NotificacaoBanco
import br.com.meubolso.domain.model.Recorrencia
import br.com.meubolso.domain.model.TipoTransacao
import br.com.meubolso.domain.model.VinculoPeriodo
import br.com.meubolso.domain.vale.CalculadoraValeAlimentacao
import br.com.meubolso.domain.validacao.ErroValidacao
import br.com.meubolso.ui.navigation.Rotas
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import javax.inject.Inject
import kotlin.math.abs

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
    /** Despesa lançada em fatura de cartão em vez de despesa fixa. */
    val emFatura: Boolean = false,
    val idCompra: Long? = null,
    val cartaoId: Long? = null,
    val dataCompra: LocalDate,
    val modalidade: ModalidadeCompra = ModalidadeCompra.A_VISTA,
    /** Crédito na fatura: [valorCentavos] fica positivo no formulário e é gravado negativo. */
    val estorno: Boolean = false,
    /** Só para compra fixa: `false` quando a recorrência foi encerrada. */
    val fixaAtiva: Boolean = true,
    val mesUltimaFatura: YearMonth? = null,
    /** Lançamento vindo de uma notificação de banco: ao salvar, sai da caixa de pendentes. */
    val pendenteId: Long? = null,
    /** Texto original da notificação, para o usuário conferir. */
    val textoNotificacao: String? = null,
    val erros: Map<CampoDespesa, ErroValidacao> = emptyMap(),
    val concluido: Boolean = false,
) {
    val editando: Boolean get() = id != null || idCompra != null
}

/**
 * Efeito de uma despesa paga com VR/VA no saldo do vale do [mes].
 * [disponivelCentavos] já desconta as outras despesas do mês (não esta).
 */
data class AvisoVale(
    val mes: YearMonth,
    val possuiCredito: Boolean,
    val disponivelCentavos: Long,
    val saldoAposCentavos: Long,
) {
    val ficaraNegativo: Boolean get() = saldoAposCentavos < 0
}

/** Em qual fatura a compra entra e, no parcelado, quanto fica cada parcela. */
data class PreviaFatura(
    val mes: YearMonth,
    val fechamento: LocalDate,
    /** Mês já gravado na compra (edição sem mudar cartão nem data). */
    val jaLancada: Boolean,
    val parcelas: List<Long>?,
)

@HiltViewModel
class DespesaFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val despesaRepository: DespesaRepository,
    private val cartaoRepository: CartaoRepository,
    private val notificacaoRepository: NotificacaoBancoRepository,
    categoriaRepository: CategoriaRepository,
    rendaRepository: RendaRepository,
    private val calculadora: CalculadoraFatura,
    private val calculadoraVale: CalculadoraValeAlimentacao,
    private val clock: Clock,
) : ViewModel() {

    private val idDespesa: Long? = savedStateHandle.get<Long>(Rotas.ARG_ID)?.takeIf { it > 0 }
    private val idCompra: Long? = savedStateHandle.get<Long>(Rotas.ARG_COMPRA_ID)?.takeIf { it > 0 }
    private val cartaoInicial: Long? = savedStateHandle.get<Long>(Rotas.ARG_CARTAO_ID)?.takeIf { it > 0 }
    private val pendenteId: Long? = savedStateHandle.get<Long>(Rotas.ARG_PENDENTE_ID)?.takeIf { it > 0 }

    /** Compra como estava gravada, para preservar a fatura de origem na edição. */
    private var compraOriginal: CompraCartao? = null

    private val _uiState = MutableStateFlow(
        DespesaFormUiState(
            id = idDespesa,
            mesInicio = YearMonth.now(clock),
            emFatura = idCompra != null || cartaoInicial != null,
            idCompra = idCompra,
            cartaoId = cartaoInicial,
            dataCompra = LocalDate.now(clock),
            pendenteId = pendenteId,
        ),
    )
    val uiState: StateFlow<DespesaFormUiState> = _uiState.asStateFlow()

    val categorias: StateFlow<List<Categoria>> = categoriaRepository.observarCategorias()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val cartoes: StateFlow<List<Cartao>> = cartaoRepository.observarCartoes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val previaFatura: StateFlow<PreviaFatura?> = combine(_uiState, cartoes) { estado, cartoes ->
        val cartao = cartoes.firstOrNull { it.id == estado.cartaoId } ?: return@combine null
        val mes = mesPrimeiraFatura(estado, cartao)
        PreviaFatura(
            mes = mes,
            fechamento = calculadora.fechamento(cartao, mes),
            jaLancada = !mudouOrigem(estado),
            parcelas = estado.totalParcelas.toIntOrNull()
                ?.takeIf { estado.modalidade == ModalidadeCompra.PARCELADO && it in ValidadorDespesa.INTERVALO_PARCELAS }
                ?.let { calculadora.dividirEmParcelas(estado.valorCentavos, it) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * Só quando a forma é VR/VA. Mês considerado: o da cobrança na despesa única; nas demais,
     * o mês atual (ou o de início, se ainda não começou). Todas as despesas do mês contam,
     * pagas ou pendentes, como no painel.
     */
    val avisoVale: StateFlow<AvisoVale?> = combine(
        _uiState,
        rendaRepository.observarRendas(),
        despesaRepository.observarDespesas(),
    ) { estado, rendas, despesas ->
        if (estado.emFatura || estado.formaPagamento != FormaPagamento.VALE_ALIMENTACAO) return@combine null
        val mes = if (estado.recorrencia == Recorrencia.UNICA) {
            estado.mesInicio
        } else {
            maxOf(estado.mesInicio, YearMonth.now(clock))
        }
        val outras = despesas.filter { it.id != estado.id }
        val saldo = calculadoraVale.saldoNoMes(rendas, outras, emptyList(), mes)
        AvisoVale(
            mes = mes,
            possuiCredito = saldo.creditos.isNotEmpty(),
            disponivelCentavos = saldo.saldoCentavos,
            saldoAposCentavos = saldo.saldoCentavos - estado.valorCentavos,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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
        if (pendenteId != null) {
            viewModelScope.launch { preencherComNotificacao(pendenteId) }
        }
        if (idCompra != null) {
            viewModelScope.launch {
                cartaoRepository.buscarCompra(idCompra)?.let { compra ->
                    compraOriginal = compra
                    _uiState.update {
                        it.copy(
                            descricao = compra.descricao,
                            categoriaId = compra.categoriaId,
                            valorCentavos = abs(compra.valorCentavos),
                            estorno = compra.estorno,
                            totalParcelas = compra.totalParcelas?.toString().orEmpty(),
                            cartaoId = compra.cartaoId,
                            dataCompra = compra.dataCompra,
                            modalidade = compra.modalidade,
                            fixaAtiva = compra.mesUltimaFatura == null,
                            mesUltimaFatura = compra.mesUltimaFatura,
                        )
                    }
                }
            }
        }
    }

    /**
     * Pré-preenche com o que foi lido da notificação. Compra no cartão já abre como fatura
     * (com o cartão selecionado, se houver só um); o resto, como despesa única já paga.
     * O usuário pode trocar entre despesa e fatura antes de salvar.
     */
    private suspend fun preencherComNotificacao(id: Long) {
        val notificacao = notificacaoRepository.buscar(id) ?: return
        val data = LocalDateTime.ofInstant(notificacao.recebidaEm, clock.zone).toLocalDate()
        val emFatura = notificacao.tipo == TipoTransacao.CARTAO && !notificacao.vale
        val cartaoUnico = if (emFatura) cartaoRepository.observarCartoes().first().singleOrNull()?.id else null
        _uiState.update {
            it.copy(
                descricao = notificacao.descricaoSugerida ?: notificacao.banco,
                valorCentavos = notificacao.valorCentavos ?: 0,
                dia = data.dayOfMonth.toString(),
                formaPagamento = formaPagamentoSugerida(notificacao),
                recorrencia = Recorrencia.UNICA,
                mesInicio = YearMonth.from(data),
                emFatura = emFatura,
                cartaoId = cartaoUnico,
                dataCompra = data,
                textoNotificacao = listOf(notificacao.titulo, notificacao.texto).filter(String::isNotBlank).joinToString("\n"),
            )
        }
    }

    /** Compra no crédito vai para a fatura; como despesa, o padrão é Pix (o usuário ajusta). */
    private fun formaPagamentoSugerida(notificacao: NotificacaoBanco): FormaPagamento =
        if (notificacao.vale) FormaPagamento.VALE_ALIMENTACAO else FormaPagamento.PIX

    fun alterarDescricao(valor: String) = atualizar(CampoDespesa.DESCRICAO) { copy(descricao = valor) }

    fun alterarCategoria(categoria: Categoria) = atualizar(CampoDespesa.CATEGORIA) { copy(categoriaId = categoria.id) }

    fun alterarValor(centavos: Long) = atualizar(CampoDespesa.VALOR) { copy(valorCentavos = centavos) }

    fun alterarDia(valor: String) = atualizar(CampoDespesa.DIA) { copy(dia = valor.filter(Char::isDigit).take(2)) }

    fun alterarFormaPagamento(valor: FormaPagamento) = _uiState.update { it.copy(formaPagamento = valor) }

    fun alterarVinculo(valor: VinculoPeriodo) = _uiState.update { it.copy(vinculo = valor) }

    fun alterarRecorrencia(valor: Recorrencia) = atualizar(CampoDespesa.PARCELAS) { copy(recorrencia = valor) }

    fun alterarTotalParcelas(valor: String) =
        atualizar(CampoDespesa.PARCELAS, CampoDespesa.VALOR) { copy(totalParcelas = valor.filter(Char::isDigit).take(3)) }

    fun alterarMesInicio(valor: YearMonth) = _uiState.update { it.copy(mesInicio = valor) }

    fun alterarAtiva(valor: Boolean) = _uiState.update { it.copy(ativa = valor) }

    /** Com um único cartão cadastrado, já o deixa selecionado. */
    fun alterarEmFatura(valor: Boolean) = _uiState.update {
        it.copy(emFatura = valor, cartaoId = it.cartaoId ?: cartoes.value.singleOrNull()?.id, erros = emptyMap())
    }

    fun alterarCartao(cartao: Cartao) = atualizar(CampoDespesa.CARTAO, CampoDespesa.ENCERRAMENTO) { copy(cartaoId = cartao.id) }

    fun alterarDataCompra(valor: LocalDate?) {
        if (valor != null) atualizar(CampoDespesa.ENCERRAMENTO) { copy(dataCompra = valor) }
    }

    fun alterarModalidade(valor: ModalidadeCompra) =
        atualizar(CampoDespesa.PARCELAS, CampoDespesa.VALOR, CampoDespesa.ENCERRAMENTO) { copy(modalidade = valor) }

    /** Estorno não é recorrente: se a compra estava como fixa, volta para à vista. */
    fun alterarEstorno(valor: Boolean) = atualizar(CampoDespesa.ENCERRAMENTO) {
        copy(
            estorno = valor,
            modalidade = if (valor && modalidade == ModalidadeCompra.FIXO) ModalidadeCompra.A_VISTA else modalidade,
        )
    }

    /** Ao encerrar, sugere como última a fatura aberta hoje (a cobrança deste mês ainda vale). */
    fun alterarFixaAtiva(valor: Boolean) = atualizar(CampoDespesa.ENCERRAMENTO) {
        val cartao = cartoes.value.firstOrNull { it.id == cartaoId }
        val sugestao = cartao?.let { c ->
            maxOf(calculadora.faturaDaCompra(c, LocalDate.now(clock)), mesPrimeiraFatura(this, c))
        }
        copy(fixaAtiva = valor, mesUltimaFatura = if (valor) null else mesUltimaFatura ?: sugestao)
    }

    fun alterarMesUltimaFatura(valor: YearMonth) =
        atualizar(CampoDespesa.ENCERRAMENTO) { copy(mesUltimaFatura = valor) }

    fun salvar() {
        if (_uiState.value.emFatura) salvarCompra() else salvarDespesaFixa()
    }

    private fun salvarDespesaFixa() {
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
            if (pendenteId != null) {
                // A notificação é de algo já pago: a 1ª ocorrência entra como paga na data da compra.
                notificacaoRepository.confirmarComDespesa(pendenteId, despesa, dataPagamento = estado.dataCompra)
            } else {
                despesaRepository.salvar(despesa)
            }
            _uiState.update { it.copy(concluido = true) }
        }
    }

    private fun salvarCompra() {
        val estado = _uiState.value
        val cartao = cartoes.value.firstOrNull { it.id == estado.cartaoId }
        val parcelas = estado.totalParcelas.toIntOrNull()
        val primeira = cartao?.let { mesPrimeiraFatura(estado, it) }
        val ultima = if (estado.modalidade == ModalidadeCompra.FIXO && !estado.fixaAtiva) estado.mesUltimaFatura else null
        val erros = ValidadorCompraCartao.validar(
            cartaoId = cartao?.id,
            descricao = estado.descricao,
            categoriaId = estado.categoriaId,
            valorCentavos = estado.valorCentavos,
            modalidade = estado.modalidade,
            totalParcelas = parcelas,
            mesPrimeiraFatura = primeira,
            mesUltimaFatura = ultima,
        )
        if (erros.isNotEmpty()) {
            _uiState.update { it.copy(erros = erros) }
            return
        }
        val compra = CompraCartao(
            id = estado.idCompra ?: 0,
            cartaoId = checkNotNull(cartao).id,
            descricao = estado.descricao.trim(),
            categoriaId = checkNotNull(estado.categoriaId),
            valorCentavos = if (estado.estorno) -estado.valorCentavos else estado.valorCentavos,
            dataCompra = estado.dataCompra,
            modalidade = estado.modalidade,
            totalParcelas = if (estado.modalidade == ModalidadeCompra.PARCELADO) parcelas else null,
            mesPrimeiraFatura = checkNotNull(primeira),
            mesUltimaFatura = ultima,
        )
        viewModelScope.launch {
            if (pendenteId != null) {
                notificacaoRepository.confirmarComCompra(pendenteId, compra)
            } else {
                cartaoRepository.salvarCompra(compra)
            }
            _uiState.update { it.copy(concluido = true) }
        }
    }

    fun excluir() {
        viewModelScope.launch {
            when {
                idCompra != null -> cartaoRepository.excluirCompra(idCompra)
                idDespesa != null -> despesaRepository.excluir(idDespesa)
                else -> return@launch
            }
            _uiState.update { it.copy(concluido = true) }
        }
    }

    /**
     * Na edição, mantém a fatura gravada enquanto cartão e data não mudam: assim alterar o
     * fechamento do cartão depois não move compras antigas.
     */
    private fun mesPrimeiraFatura(estado: DespesaFormUiState, cartao: Cartao): YearMonth {
        val original = compraOriginal
        return if (original != null && !mudouOrigem(estado)) {
            original.mesPrimeiraFatura
        } else {
            calculadora.faturaDaCompra(cartao, estado.dataCompra)
        }
    }

    private fun mudouOrigem(estado: DespesaFormUiState): Boolean {
        val original = compraOriginal ?: return true
        return original.cartaoId != estado.cartaoId || original.dataCompra != estado.dataCompra
    }

    /** Aplica a alteração e limpa o erro dos campos afetados. */
    private fun atualizar(vararg campos: CampoDespesa, alteracao: DespesaFormUiState.() -> DespesaFormUiState) {
        _uiState.update { it.alteracao().copy(erros = it.erros - campos.toSet()) }
    }
}
