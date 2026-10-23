package br.com.meubolso.data.local.categoria

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoriaDao {

    @Query("SELECT * FROM categorias ORDER BY nome COLLATE NOCASE")
    fun observarTodas(): Flow<List<CategoriaEntity>>

    @Upsert
    suspend fun salvar(categoria: CategoriaEntity)

    @Query("DELETE FROM categorias WHERE id = :id AND padrao = 0")
    suspend fun excluir(id: Long)

    /** Despesas fixas e compras de cartão que usam a categoria. */
    @Query(
        """
        SELECT (SELECT COUNT(*) FROM despesas_fixas WHERE categoria_id = :id)
             + (SELECT COUNT(*) FROM compras_cartao WHERE categoria_id = :id)
        """,
    )
    suspend fun contarDespesas(id: Long): Int
}
