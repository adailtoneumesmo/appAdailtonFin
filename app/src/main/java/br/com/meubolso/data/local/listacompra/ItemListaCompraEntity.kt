package br.com.meubolso.data.local.listacompra

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import br.com.meubolso.domain.model.ItemListaCompra

/** Excluir a lista remove os itens em cascata. */
@Entity(
    tableName = "itens_lista_compra",
    foreignKeys = [
        ForeignKey(
            entity = ListaCompraEntity::class,
            parentColumns = ["id"],
            childColumns = ["lista_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("lista_id")],
)
data class ItemListaCompraEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "lista_id") val listaId: Long,
    val nome: String,
    @ColumnInfo(name = "valor_centavos") val valorCentavos: Long,
    @ColumnInfo(name = "quantidade_milesimos") val quantidadeMilesimos: Long,
    @ColumnInfo(name = "no_carrinho") val noCarrinho: Boolean,
    val ordem: Int,
)

fun ItemListaCompraEntity.paraDominio() = ItemListaCompra(
    id = id,
    listaId = listaId,
    nome = nome,
    valorCentavos = valorCentavos,
    quantidadeMilesimos = quantidadeMilesimos,
    noCarrinho = noCarrinho,
    ordem = ordem,
)

fun ItemListaCompra.paraEntity() = ItemListaCompraEntity(
    id = id,
    listaId = listaId,
    nome = nome,
    valorCentavos = valorCentavos,
    quantidadeMilesimos = quantidadeMilesimos,
    noCarrinho = noCarrinho,
    ordem = ordem,
)
