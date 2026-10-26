package br.com.meubolso.ui.listacompras

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.meubolso.data.repository.CartaoRepository
import br.com.meubolso.data.repository.CategoriaRepository
import br.com.meubolso.data.repository.DespesaRepository
import br.com.meubolso.data.repository.ListaCompraRepository
import br.com.meubolso.data.repository.RendaRepository
import br.com.meubolso.domain.cartao.CalculadoraFatura
import br.com.meubolso.domain.listacompra.CalculadoraListaCompra
import br.com.meubolso.domain.listacompra.CampoListaCompra
import br.com.meubolso.domain.listacompra.ResumoListaCompra
import br.com.meubolso.domain.listacompra.ValidadorListaCompra
import br.com.meubolso.domain.model.Cartao
import br.com.meubolso.domain.model.Categoria
import br.com.meubolso.domain.model.CompraCartao
import br.com.meubolso.domain.model.DespesaFixa
import br.com.meubolso.domain.model.FormaPagamento
import br.com.meubolso.domain.model.ItemListaCompra
import br.com.meubolso.domain.model.ListaCompra
import br.com.meubolso.domain.model.ModalidadeCompra
import br.com.meubolso.domain.model.Recorrencia
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

data class ListaCompraItensUiState(
    val carregando: Boolean = true,
    val lista: ListaCompra? = null,
    val itens: List<ItemListaCompra> = emptyList(),
    val resumo: ResumoListaCompra = ResumoListaCompra(),
)

/** Campos de adição rápida no topo da tela. */
data class NovoItemUiState(
    val nome: String = "",
    val valorCentavos: Long = 0,
    val quantidade: String = "",
    val erros: Map<CampoListaCompra, ErroValidacao> = emptyMap(),
)

/** Escolhas do diálogo "Finalizar compra". */
data class FinalizacaoUi(
    val usarCarrinho: Boolean,
    val categoriaId: Long?,
    val formaPagamento: FormaPagamento = FormaPagamento.PIX,
    val cartaoId: Long? = null,
)

@HiltViewModel
class ListaCompraItensViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ListaCompraRepository,
    categoriaRepository: CategoriaRepository,
    cartaoRepository: CartaoRepository,
    rendaRepository: RendaRepository,
    despesaRepository: DespesaRepository,
    private val calculadoraFatura: CalculadoraFatura,
    private val calculadoraVale: CalculadoraValeAlimentacao,
) : ViewModel() {

    private val idLista: Long = checkNotNull(savedStateHandle.get<Long>(Rotas.ARG_ID)?.takeIf { it > 0 })

    val uiState: StateFlow<ListaCompraItensUiState> = combine(
        repository.observarLista(idLista),
        repository.observarItens(idLista),
    ) { lista, itens ->
        ListaCompraItensUiState(
            carregando = false,
            lista = lista,
            itens = itens,
            resumo = CalculadoraListaCompra.resumir(itens, lista?.limiteCentavos),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ListaCompraItensUiState())

    private val _novoItem = MutableStateFlow(NovoItemUiState())
    val novoItem: StateFlow<NovoItemUiState> = _novoItem.asStateFlow()

    val categorias: StateFlow<List<Categoria>> = categoriaRepository.observarCategorias()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val cartoes: StateFlow<List<Cartao>> = cartaoRepository.observarCartoes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Saldo do VR/VA no mês da lista, para avisar ao finalizar com o vale. */
    val saldoVale: StateFlow<SugestaoVale?> = combine(
        uiState,
        rendaRepository.observarRendas(),
        despesaRepository.observarDespesas(),
    ) { estado, rendas, despesas ->
        val lista = estado.lista ?: return@combine null
        val mes = YearMonth.from(lista.data)
        SugestaoVale(mes, calculadoraVale.saldoNoMes(rendas, despesas, emptyList(), mes))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun alterarNovoNome(valor: String) = atualizarNovo(CampoListaCompra.NOME_ITEM) { copy(nome = valor) }

    fun alterarNovoValor(centavos: Long) = _novoItem.update { it.copy(valorCentavos = centavos) }

    fun alterarNovaQuantidade(valor: String) =
        atualizarNovo(CampoListaCompra.QUANTIDADE) { copy(quantidade = filtrarQuantidade(valor)) }

    /** Retorna `true` quando o item foi incluído (a tela volta o foco para o nome). */
    fun adicionarItem(): Boolean {
        val novo = _novoItem.value
        val erros = ValidadorListaCompra.validarItem(novo.nome, novo.quantidade)
        if (erros.isNotEmpty()) {
            _novoItem.update { it.copy(erros = erros) }
            return false
        }
        val item = ItemListaCompra(
            listaId = idLista,
            nome = novo.nome.trim(),
            valorCentavos = novo.valorCentavos,
            quantidadeMilesimos = checkNotNull(CalculadoraListaCompra.quantidadeDeTexto(novo.quantidade)),
        )
        _novoItem.value = NovoItemUiState()
        viewModelScope.launch { repository.adicionarItem(item) }
        return true
    }

    /** Edição direta na linha; a tela só chama com nome preenchido e quantidade válida. */
    fun alterarItem(item: ItemListaCompra) {
        viewModelScope.launch { repository.salvarItem(item) }
    }

    fun alternarCarrinho(item: ItemListaCompra) = alterarItem(item.copy(noCarrinho = !item.noCarrinho))

    fun excluirItem(item: ItemListaCompra) {
        viewModelScope.launch { repository.excluirItem(item.id) }
    }

    /** Sugestão inicial do diálogo: itens marcados (se houver) e categoria de mercado/alimentação. */
    fun finalizacaoInicial(): FinalizacaoUi {
        val categorias = categorias.value
        val categoria = CATEGORIAS_SUGERIDAS.firstNotNullOfOrNull { nome ->
            categorias.firstOrNull { it.nome.equals(nome, ignoreCase = true) }
        } ?: categorias.firstOrNull()
        return FinalizacaoUi(
            usarCarrinho = uiState.value.resumo.carrinhoCentavos > 0,
            categoriaId = categoria?.id,
            cartaoId = cartoes.value.singleOrNull()?.id,
        )
    }

    fun valorFinalizacao(escolha: FinalizacaoUi): Long =
        uiState.value.resumo.let { if (escolha.usarCarrinho) it.carrinhoCentavos else it.totalCentavos }

    /**
     * Gera a despesa única já paga na data da lista (ou compra à vista no cartão) e marca a
     * lista como finalizada. O repositório ignora se a lista já estiver finalizada.
     */
    fun finalizar(escolha: FinalizacaoUi) {
        val lista = uiState.value.lista ?: return
        val valor = valorFinalizacao(escolha)
        val categoriaId = escolha.categoriaId ?: return
        if (valor <= 0 || lista.finalizada) return
        viewModelScope.launch {
            if (escolha.formaPagamento == FormaPagamento.CARTAO_CREDITO) {
                val cartao = cartoes.value.firstOrNull { it.id == escolha.cartaoId } ?: return@launch
                repository.finalizarComCompra(
                    lista.id,
                    CompraCartao(
                        cartaoId = cartao.id,
                        descricao = lista.descricao,
                        categoriaId = categoriaId,
                        valorCentavos = valor,
                        dataCompra = lista.data,
                        modalidade = ModalidadeCompra.A_VISTA,
                        mesPrimeiraFatura = calculadoraFatura.faturaDaCompra(cartao, lista.data),
                    ),
                )
            } else {
                repository.finalizarComDespesa(
                    lista.id,
                    DespesaFixa(
                        descricao = lista.descricao,
                        categoriaId = categoriaId,
                        valorCentavos = valor,
                        diaVencimento = lista.data.dayOfMonth,
                        formaPagamento = escolha.formaPagamento,
                        vinculo = VinculoPeriodo.AUTOMATICO,
                        recorrencia = Recorrencia.UNICA,
                        mesInicio = YearMonth.from(lista.data),
                    ),
                    dataPagamento = lista.data,
                )
            }
        }
    }

    private fun atualizarNovo(vararg campos: CampoListaCompra, alteracao: NovoItemUiState.() -> NovoItemUiState) {
        _novoItem.update { it.alteracao().copy(erros = it.erros - campos.toSet()) }
    }

    companion object {
        private val CATEGORIAS_SUGERIDAS = listOf("Mercado", "Alimentação")

        /** Mantém só dígitos e um separador decimal, até 3 casas. */
        fun filtrarQuantidade(texto: String): String {
            val filtrado = texto.filter { it.isDigit() || it == ',' || it == '.' }.replace('.', ',')
            val partes = filtrado.split(',')
            return if (partes.size == 1) partes[0].take(6) else partes[0].take(6) + "," + partes.drop(1).joinToString("").take(3)
        }
    }
}
