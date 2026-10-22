package br.com.meubolso.data.local.despesa

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth

@Dao
interface DespesaDao {

    @Query("SELECT * FROM despesas_fixas ORDER BY descricao COLLATE NOCASE")
    fun observarTodas(): Flow<List<DespesaFixaEntity>>

    @Query("SELECT * FROM despesas_fixas WHERE id = :id")
    suspend fun buscarPorId(id: Long): DespesaFixaEntity?

    @Upsert
    suspend fun salvar(despesa: DespesaFixaEntity)

    @Query("DELETE FROM despesas_fixas WHERE id = :id")
    suspend fun excluir(id: Long)

    /** Competências gravadas como "yyyy-MM", então a comparação textual respeita a ordem cronológica. */
    @Query("SELECT * FROM pagamentos_despesa WHERE competencia BETWEEN :de AND :ate")
    fun observarPagamentos(de: YearMonth, ate: YearMonth): Flow<List<PagamentoDespesaEntity>>

    @Upsert
    suspend fun salvarPagamento(pagamento: PagamentoDespesaEntity)

    @Query("DELETE FROM pagamentos_despesa WHERE despesa_id = :despesaId AND competencia = :competencia")
    suspend fun excluirPagamento(despesaId: Long, competencia: YearMonth)
}
