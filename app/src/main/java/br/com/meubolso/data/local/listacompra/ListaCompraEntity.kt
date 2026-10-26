package br.com.meubolso.data.local.listacompra

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import br.com.meubolso.data.local.cartao.CompraCartaoEntity
import br.com.meubolso.data.local.despesa.DespesaFixaEntity
import br.com.meubolso.domain.model.ListaCompra
import java.time.LocalDate

/** Excluir a despesa (ou compra) gerada só desfaz o vínculo: a lista volta a ficar em aberto. */
@Entity(
    tableName = "listas_compra",
    foreignKeys = [
        ForeignKey(
            entity = DespesaFixaEntity::class,
            parentColumns = ["id"],
            childColumns = ["despesa_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = CompraCartaoEntity::class,
            parentColumns = ["id"],
            childColumns = ["compra_cartao_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("data"), Index("despesa_id"), Index("compra_cartao_id")],
)
data class ListaCompraEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val descricao: String,
    val data: LocalDate,
    @ColumnInfo(name = "limite_centavos") val limiteCentavos: Long?,
    @ColumnInfo(name = "despesa_id") val despesaId: Long?,
    @ColumnInfo(name = "compra_cartao_id") val compraCartaoId: Long?,
)

fun ListaCompraEntity.paraDominio() = ListaCompra(
    id = id,
    descricao = descricao,
    data = data,
    limiteCentavos = limiteCentavos,
    despesaId = despesaId,
    compraCartaoId = compraCartaoId,
)

fun ListaCompra.paraEntity() = ListaCompraEntity(
    id = id,
    descricao = descricao,
    data = data,
    limiteCentavos = limiteCentavos,
    despesaId = despesaId,
    compraCartaoId = compraCartaoId,
)
