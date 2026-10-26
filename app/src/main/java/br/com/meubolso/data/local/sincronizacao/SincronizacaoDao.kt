package br.com.meubolso.data.local.sincronizacao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SincronizacaoDao {

    @Query("SELECT COUNT(*) FROM sync_registro WHERE pendente = 1")
    fun observarQuantidadePendentes(): Flow<Int>

    @Query("SELECT * FROM sync_registro WHERE pendente = 1")
    suspend fun buscarPendentes(): List<RegistroSincronizacaoEntity>

    /** Só limpa se a linha não mudou de novo enquanto era enviada. */
    @Query("UPDATE sync_registro SET pendente = 0 WHERE uuid = :uuid AND atualizado_em = :atualizadoEm")
    suspend fun marcarEnviado(uuid: String, atualizadoEm: Long)

    /** Exclusão já enviada: a linha local não existe mais, então o registro pode sair. */
    @Query("DELETE FROM sync_registro WHERE uuid = :uuid AND excluido = 1 AND atualizado_em = :atualizadoEm")
    suspend fun removerExclusaoEnviada(uuid: String, atualizadoEm: Long)

    @Query("SELECT servidor_millis FROM sync_cursor WHERE colecao = :colecao")
    suspend fun cursor(colecao: String): Long?

    @Upsert
    suspend fun salvarCursor(cursor: CursorSincronizacaoEntity)
}
