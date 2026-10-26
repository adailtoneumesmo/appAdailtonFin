package br.com.meubolso.data.local.notificacao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.meubolso.domain.model.StatusNotificacao
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificacaoBancoDao {

    /** Ignora repetidas (mesma [NotificacaoBancoEntity.chave]), inclusive as já confirmadas ou descartadas. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun inserir(notificacao: NotificacaoBancoEntity): Long

    @Query("SELECT * FROM notificacoes_banco WHERE status = :status ORDER BY recebida_em_millis DESC")
    fun observarPorStatus(status: StatusNotificacao): Flow<List<NotificacaoBancoEntity>>

    @Query("SELECT COUNT(*) FROM notificacoes_banco WHERE status = :status")
    fun contarPorStatus(status: StatusNotificacao): Flow<Int>

    @Query("SELECT * FROM notificacoes_banco WHERE id = :id")
    suspend fun buscar(id: Long): NotificacaoBancoEntity?

    @Query("UPDATE notificacoes_banco SET status = :status WHERE id = :id")
    suspend fun alterarStatus(id: Long, status: StatusNotificacao)
}
