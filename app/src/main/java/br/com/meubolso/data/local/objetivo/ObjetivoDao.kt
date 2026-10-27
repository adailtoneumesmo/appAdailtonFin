package br.com.meubolso.data.local.objetivo

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface ObjetivoDao {

    @Query("SELECT * FROM objetivos ORDER BY nome COLLATE NOCASE")
    fun observarTodos(): Flow<List<ObjetivoEntity>>

    @Query("SELECT * FROM objetivos WHERE id = :id")
    suspend fun buscarPorId(id: Long): ObjetivoEntity?

    @Upsert
    suspend fun salvar(objetivo: ObjetivoEntity)

    @Query("DELETE FROM objetivos WHERE id = :id")
    suspend fun excluir(id: Long)

    @Query("SELECT * FROM posicoes_investimento ORDER BY data_referencia DESC, id DESC LIMIT 1")
    fun observarPosicaoAtual(): Flow<PosicaoInvestimentoEntity?>

    @Query("SELECT * FROM posicoes_investimento WHERE data_referencia = :data ORDER BY id DESC LIMIT 1")
    suspend fun buscarPosicaoDoDia(data: LocalDate): PosicaoInvestimentoEntity?

    @Upsert
    suspend fun salvarPosicao(posicao: PosicaoInvestimentoEntity)
}
