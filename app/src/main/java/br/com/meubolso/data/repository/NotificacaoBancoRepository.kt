package br.com.meubolso.data.repository

import androidx.room.withTransaction
import br.com.meubolso.data.local.MeuBolsoDatabase
import br.com.meubolso.data.local.cartao.CartaoDao
import br.com.meubolso.data.local.cartao.paraEntity
import br.com.meubolso.data.local.despesa.DespesaDao
import br.com.meubolso.data.local.despesa.PagamentoDespesaEntity
import br.com.meubolso.data.local.despesa.paraEntity
import br.com.meubolso.data.local.notificacao.NotificacaoBancoDao
import br.com.meubolso.data.local.notificacao.paraDominio
import br.com.meubolso.data.local.notificacao.paraEntity
import br.com.meubolso.domain.model.CompraCartao
import br.com.meubolso.domain.model.DespesaFixa
import br.com.meubolso.domain.model.NotificacaoBanco
import br.com.meubolso.domain.model.StatusNotificacao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificacaoBancoRepository @Inject constructor(
    private val database: MeuBolsoDatabase,
    private val dao: NotificacaoBancoDao,
    private val despesaDao: DespesaDao,
    private val cartaoDao: CartaoDao,
) {
    fun observarPendentes(): Flow<List<NotificacaoBanco>> =
        dao.observarPorStatus(StatusNotificacao.PENDENTE).map { lista -> lista.map { it.paraDominio() } }

    fun observarQuantidadePendentes(): Flow<Int> = dao.contarPorStatus(StatusNotificacao.PENDENTE)

    suspend fun buscar(id: Long): NotificacaoBanco? = dao.buscar(id)?.paraDominio()

    /** [chave] identifica a notificação no aparelho; repetidas são ignoradas. */
    suspend fun registrar(notificacao: NotificacaoBanco, chave: String) {
        dao.inserir(notificacao.paraEntity(chave))
    }

    suspend fun descartar(id: Long) = dao.alterarStatus(id, StatusNotificacao.DESCARTADA)

    /** Lança a despesa já paga em [dataPagamento] e tira a notificação dos pendentes, na mesma transação. */
    suspend fun confirmarComDespesa(id: Long, despesa: DespesaFixa, dataPagamento: LocalDate) =
        database.withTransaction {
            val despesaId = despesaDao.inserir(despesa.paraEntity())
            despesaDao.salvarPagamento(PagamentoDespesaEntity(despesaId, despesa.mesInicio, dataPagamento))
            dao.alterarStatus(id, StatusNotificacao.CONFIRMADA)
        }

    /** Igual a [confirmarComDespesa], lançando a compra na fatura do cartão. */
    suspend fun confirmarComCompra(id: Long, compra: CompraCartao) =
        database.withTransaction {
            cartaoDao.inserirCompra(compra.paraEntity())
            dao.alterarStatus(id, StatusNotificacao.CONFIRMADA)
        }
}
