package br.com.meubolso.data.local.renda

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface RendaDao {

    @Query("SELECT * FROM rendas ORDER BY descricao COLLATE NOCASE")
    fun observarTodas(): Flow<List<RendaEntity>>

    @Query("SELECT * FROM rendas WHERE id = :id")
    suspend fun buscarPorId(id: Long): RendaEntity?

    @Upsert
    suspend fun salvar(renda: RendaEntity)

    @Query("DELETE FROM rendas WHERE id = :id")
    suspend fun excluir(id: Long)
}
