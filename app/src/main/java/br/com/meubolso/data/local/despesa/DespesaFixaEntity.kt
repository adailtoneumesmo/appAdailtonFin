package br.com.meubolso.data.local.despesa

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import br.com.meubolso.data.local.categoria.CategoriaEntity
import br.com.meubolso.domain.model.DespesaFixa
import br.com.meubolso.domain.model.FormaPagamento
import br.com.meubolso.domain.model.Recorrencia
import br.com.meubolso.domain.model.VinculoPeriodo
import java.time.YearMonth

@Entity(
    tableName = "despesas_fixas",
    foreignKeys = [
        ForeignKey(
            entity = CategoriaEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoria_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("categoria_id")],
)
data class DespesaFixaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val descricao: String,
    @ColumnInfo(name = "categoria_id") val categoriaId: Long,
    @ColumnInfo(name = "valor_centavos") val valorCentavos: Long,
    @ColumnInfo(name = "dia_vencimento") val diaVencimento: Int,
    @ColumnInfo(name = "forma_pagamento") val formaPagamento: FormaPagamento,
    val vinculo: VinculoPeriodo,
    val recorrencia: Recorrencia,
    @ColumnInfo(name = "total_parcelas") val totalParcelas: Int?,
    @ColumnInfo(name = "mes_inicio") val mesInicio: YearMonth,
    val ativa: Boolean,
)

fun DespesaFixaEntity.paraDominio() = DespesaFixa(
    id = id,
    descricao = descricao,
    categoriaId = categoriaId,
    valorCentavos = valorCentavos,
    diaVencimento = diaVencimento,
    formaPagamento = formaPagamento,
    vinculo = vinculo,
    recorrencia = recorrencia,
    totalParcelas = totalParcelas,
    mesInicio = mesInicio,
    ativa = ativa,
)

fun DespesaFixa.paraEntity() = DespesaFixaEntity(
    id = id,
    descricao = descricao,
    categoriaId = categoriaId,
    valorCentavos = valorCentavos,
    diaVencimento = diaVencimento,
    formaPagamento = formaPagamento,
    vinculo = vinculo,
    recorrencia = recorrencia,
    totalParcelas = totalParcelas,
    mesInicio = mesInicio,
    ativa = ativa,
)
