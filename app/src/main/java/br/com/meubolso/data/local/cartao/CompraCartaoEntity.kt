package br.com.meubolso.data.local.cartao

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import br.com.meubolso.data.local.categoria.CategoriaEntity
import br.com.meubolso.domain.model.CompraCartao
import br.com.meubolso.domain.model.ModalidadeCompra
import java.time.LocalDate
import java.time.YearMonth

/** Excluir o cartão remove as compras dele em cascata. */
@Entity(
    tableName = "compras_cartao",
    foreignKeys = [
        ForeignKey(
            entity = CartaoEntity::class,
            parentColumns = ["id"],
            childColumns = ["cartao_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = CategoriaEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoria_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("cartao_id"), Index("categoria_id")],
)
data class CompraCartaoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "cartao_id") val cartaoId: Long,
    val descricao: String,
    @ColumnInfo(name = "categoria_id") val categoriaId: Long,
    @ColumnInfo(name = "valor_centavos") val valorCentavos: Long,
    @ColumnInfo(name = "data_compra") val dataCompra: LocalDate,
    val modalidade: ModalidadeCompra,
    @ColumnInfo(name = "total_parcelas") val totalParcelas: Int?,
    @ColumnInfo(name = "mes_primeira_fatura") val mesPrimeiraFatura: YearMonth,
    @ColumnInfo(name = "mes_ultima_fatura") val mesUltimaFatura: YearMonth?,
)

fun CompraCartaoEntity.paraDominio() = CompraCartao(
    id = id,
    cartaoId = cartaoId,
    descricao = descricao,
    categoriaId = categoriaId,
    valorCentavos = valorCentavos,
    dataCompra = dataCompra,
    modalidade = modalidade,
    totalParcelas = totalParcelas,
    mesPrimeiraFatura = mesPrimeiraFatura,
    mesUltimaFatura = mesUltimaFatura,
)

fun CompraCartao.paraEntity() = CompraCartaoEntity(
    id = id,
    cartaoId = cartaoId,
    descricao = descricao,
    categoriaId = categoriaId,
    valorCentavos = valorCentavos,
    dataCompra = dataCompra,
    modalidade = modalidade,
    totalParcelas = totalParcelas,
    mesPrimeiraFatura = mesPrimeiraFatura,
    mesUltimaFatura = mesUltimaFatura,
)
