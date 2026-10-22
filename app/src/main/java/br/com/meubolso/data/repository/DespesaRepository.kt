package br.com.meubolso.data.repository

import br.com.meubolso.data.local.despesa.DespesaDao
import br.com.meubolso.data.local.despesa.PagamentoDespesaEntity
import br.com.meubolso.data.local.despesa.paraDominio
import br.com.meubolso.data.local.despesa.paraEntity
import br.com.meubolso.domain.model.DespesaFixa
import br.com.meubolso.domain.model.PagamentoDespesa
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DespesaRepository @Inject constructor(
    private val dao: DespesaDao,
) {
    fun observarDespesas(): Flow<List<DespesaFixa>> =
        dao.observarTodas().map { lista -> lista.map { it.paraDominio() } }

    suspend fun buscar(id: Long): DespesaFixa? = dao.buscarPorId(id)?.paraDominio()

    suspend fun salvar(despesa: DespesaFixa) = dao.salvar(despesa.paraEntity())

    /** Remove a despesa e, em cascata, o histórico de pagamentos dela. */
    suspend fun excluir(id: Long) = dao.excluir(id)

    fun observarPagamentos(de: YearMonth, ate: YearMonth = de): Flow<List<PagamentoDespesa>> =
        dao.observarPagamentos(de, ate).map { lista -> lista.map { it.paraDominio() } }

    suspend fun marcarComoPaga(despesaId: Long, competencia: YearMonth, data: LocalDate) =
        dao.salvarPagamento(PagamentoDespesaEntity(despesaId, competencia, data))

    suspend fun marcarComoPendente(despesaId: Long, competencia: YearMonth) =
        dao.excluirPagamento(despesaId, competencia)
}
