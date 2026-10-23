package br.com.meubolso.data.repository

import br.com.meubolso.data.local.cartao.CartaoDao
import br.com.meubolso.data.local.cartao.paraDominio
import br.com.meubolso.data.local.cartao.paraEntity
import br.com.meubolso.domain.model.Cartao
import br.com.meubolso.domain.model.CompraCartao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CartaoRepository @Inject constructor(
    private val dao: CartaoDao,
) {
    fun observarCartoes(): Flow<List<Cartao>> =
        dao.observarTodos().map { lista -> lista.map { it.paraDominio() } }

    fun observarCartao(id: Long): Flow<Cartao?> = dao.observarPorId(id).map { it?.paraDominio() }

    suspend fun buscar(id: Long): Cartao? = dao.buscarPorId(id)?.paraDominio()

    suspend fun salvar(cartao: Cartao) = dao.salvar(cartao.paraEntity())

    /** Remove o cartão e, em cascata, todas as compras lançadas nele. */
    suspend fun excluir(id: Long) = dao.excluir(id)

    suspend fun contarCompras(cartaoId: Long): Int = dao.contarCompras(cartaoId)

    fun observarCompras(): Flow<List<CompraCartao>> =
        dao.observarCompras().map { lista -> lista.map { it.paraDominio() } }

    fun observarCompras(cartaoId: Long): Flow<List<CompraCartao>> =
        dao.observarComprasDoCartao(cartaoId).map { lista -> lista.map { it.paraDominio() } }

    suspend fun buscarCompra(id: Long): CompraCartao? = dao.buscarCompra(id)?.paraDominio()

    suspend fun salvarCompra(compra: CompraCartao) = dao.salvarCompra(compra.paraEntity())

    suspend fun excluirCompra(id: Long) = dao.excluirCompra(id)
}
