package br.com.meubolso.data.repository

import androidx.room.withTransaction
import br.com.meubolso.data.local.MeuBolsoDatabase
import br.com.meubolso.data.local.cartao.CartaoDao
import br.com.meubolso.data.local.cartao.paraEntity
import br.com.meubolso.data.local.despesa.DespesaDao
import br.com.meubolso.data.local.despesa.PagamentoDespesaEntity
import br.com.meubolso.data.local.despesa.paraEntity
import br.com.meubolso.data.local.listacompra.ListaCompraDao
import br.com.meubolso.data.local.listacompra.paraDominio
import br.com.meubolso.data.local.listacompra.paraEntity
import br.com.meubolso.domain.model.CompraCartao
import br.com.meubolso.domain.model.DespesaFixa
import br.com.meubolso.domain.model.ItemListaCompra
import br.com.meubolso.domain.model.ListaCompra
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListaCompraRepository @Inject constructor(
    private val database: MeuBolsoDatabase,
    private val dao: ListaCompraDao,
    private val despesaDao: DespesaDao,
    private val cartaoDao: CartaoDao,
) {
    fun observarListas(mes: YearMonth): Flow<List<ListaCompra>> =
        dao.observarListas(mes.atDay(1), mes.atEndOfMonth()).map { lista -> lista.map { it.paraDominio() } }

    /** Itens de todas as listas do [mes], para montar os totais dos cards. */
    fun observarItensDoMes(mes: YearMonth): Flow<List<ItemListaCompra>> =
        dao.observarItensDoPeriodo(mes.atDay(1), mes.atEndOfMonth()).map { lista -> lista.map { it.paraDominio() } }

    fun observarLista(id: Long): Flow<ListaCompra?> = dao.observarLista(id).map { it?.paraDominio() }

    suspend fun buscarLista(id: Long): ListaCompra? = dao.buscarLista(id)?.paraDominio()

    /** Inclui ou atualiza; retorna o id da lista. */
    suspend fun salvarLista(lista: ListaCompra): Long =
        if (lista.id == 0L) dao.inserirLista(lista.paraEntity()) else lista.id.also { dao.salvarLista(lista.paraEntity()) }

    /** Remove a lista e, em cascata, os itens. A despesa gerada continua cadastrada. */
    suspend fun excluirLista(id: Long) = dao.excluirLista(id)

    fun observarItens(listaId: Long): Flow<List<ItemListaCompra>> =
        dao.observarItens(listaId).map { lista -> lista.map { it.paraDominio() } }

    /** Novo item entra no fim da lista. */
    suspend fun adicionarItem(item: ItemListaCompra) =
        dao.salvarItem(item.copy(ordem = dao.proximaOrdem(item.listaId)).paraEntity())

    suspend fun salvarItem(item: ItemListaCompra) = dao.salvarItem(item.paraEntity())

    suspend fun excluirItem(id: Long) = dao.excluirItem(id)

    /** Copia a lista para [data] com os mesmos itens, todos fora do carrinho e sem despesa vinculada. */
    suspend fun duplicar(listaId: Long, data: LocalDate): Long = database.withTransaction {
        val original = checkNotNull(dao.buscarLista(listaId)) { "Lista $listaId não encontrada" }
        val novaId = dao.inserirLista(original.copy(id = 0, data = data, despesaId = null, compraCartaoId = null))
        dao.inserirItens(dao.buscarItens(listaId).map { it.copy(id = 0, listaId = novaId, noCarrinho = false) })
        novaId
    }

    /**
     * Lança a despesa (já paga) e vincula à lista na mesma transação. Se a lista já estiver
     * finalizada, não faz nada e retorna `false` — evita gerar a despesa duas vezes.
     */
    suspend fun finalizarComDespesa(listaId: Long, despesa: DespesaFixa, dataPagamento: LocalDate): Boolean =
        database.withTransaction {
            val lista = dao.buscarLista(listaId)?.paraDominio() ?: return@withTransaction false
            if (lista.finalizada) return@withTransaction false
            val despesaId = despesaDao.inserir(despesa.paraEntity())
            despesaDao.salvarPagamento(PagamentoDespesaEntity(despesaId, despesa.mesInicio, dataPagamento))
            dao.salvarLista(lista.copy(despesaId = despesaId).paraEntity())
            true
        }

    /** Igual a [finalizarComDespesa], lançando uma compra à vista no cartão. */
    suspend fun finalizarComCompra(listaId: Long, compra: CompraCartao): Boolean =
        database.withTransaction {
            val lista = dao.buscarLista(listaId)?.paraDominio() ?: return@withTransaction false
            if (lista.finalizada) return@withTransaction false
            val compraId = cartaoDao.inserirCompra(compra.paraEntity())
            dao.salvarLista(lista.copy(compraCartaoId = compraId).paraEntity())
            true
        }
}
