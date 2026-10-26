package br.com.meubolso.data.local.cartao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CartaoDao {

    @Query("SELECT * FROM cartoes ORDER BY nome COLLATE NOCASE")
    fun observarTodos(): Flow<List<CartaoEntity>>

    @Query("SELECT * FROM cartoes WHERE id = :id")
    fun observarPorId(id: Long): Flow<CartaoEntity?>

    @Query("SELECT * FROM cartoes WHERE id = :id")
    suspend fun buscarPorId(id: Long): CartaoEntity?

    @Upsert
    suspend fun salvar(cartao: CartaoEntity)

    @Query("DELETE FROM cartoes WHERE id = :id")
    suspend fun excluir(id: Long)

    @Query("SELECT COUNT(*) FROM compras_cartao WHERE cartao_id = :cartaoId")
    suspend fun contarCompras(cartaoId: Long): Int

    @Query("SELECT * FROM compras_cartao")
    fun observarCompras(): Flow<List<CompraCartaoEntity>>

    @Query("SELECT * FROM compras_cartao WHERE cartao_id = :cartaoId")
    fun observarComprasDoCartao(cartaoId: Long): Flow<List<CompraCartaoEntity>>

    @Query("SELECT * FROM compras_cartao WHERE id = :id")
    suspend fun buscarCompra(id: Long): CompraCartaoEntity?

    @Upsert
    suspend fun salvarCompra(compra: CompraCartaoEntity)

    @Insert
    suspend fun inserirCompra(compra: CompraCartaoEntity): Long

    @Query("DELETE FROM compras_cartao WHERE id = :id")
    suspend fun excluirCompra(id: Long)
}
