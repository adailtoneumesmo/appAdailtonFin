package br.com.meubolso.data.local.listacompra

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface ListaCompraDao {

    /** Datas gravadas como "yyyy-MM-dd": a comparação textual respeita a ordem cronológica. */
    @Query("SELECT * FROM listas_compra WHERE data BETWEEN :de AND :ate ORDER BY data DESC, id DESC")
    fun observarListas(de: LocalDate, ate: LocalDate): Flow<List<ListaCompraEntity>>

    @Query("SELECT * FROM listas_compra WHERE id = :id")
    fun observarLista(id: Long): Flow<ListaCompraEntity?>

    @Query("SELECT * FROM listas_compra WHERE id = :id")
    suspend fun buscarLista(id: Long): ListaCompraEntity?

    @Insert
    suspend fun inserirLista(lista: ListaCompraEntity): Long

    @Upsert
    suspend fun salvarLista(lista: ListaCompraEntity)

    @Query("DELETE FROM listas_compra WHERE id = :id")
    suspend fun excluirLista(id: Long)

    @Query(
        """
        SELECT itens_lista_compra.* FROM itens_lista_compra
        INNER JOIN listas_compra ON listas_compra.id = itens_lista_compra.lista_id
        WHERE listas_compra.data BETWEEN :de AND :ate
        """,
    )
    fun observarItensDoPeriodo(de: LocalDate, ate: LocalDate): Flow<List<ItemListaCompraEntity>>

    @Query("SELECT * FROM itens_lista_compra WHERE lista_id = :listaId ORDER BY ordem, id")
    fun observarItens(listaId: Long): Flow<List<ItemListaCompraEntity>>

    @Query("SELECT * FROM itens_lista_compra WHERE lista_id = :listaId ORDER BY ordem, id")
    suspend fun buscarItens(listaId: Long): List<ItemListaCompraEntity>

    @Query("SELECT COALESCE(MAX(ordem), -1) + 1 FROM itens_lista_compra WHERE lista_id = :listaId")
    suspend fun proximaOrdem(listaId: Long): Int

    @Upsert
    suspend fun salvarItem(item: ItemListaCompraEntity)

    @Insert
    suspend fun inserirItens(itens: List<ItemListaCompraEntity>)

    @Query("DELETE FROM itens_lista_compra WHERE id = :id")
    suspend fun excluirItem(id: Long)
}
