package br.com.meubolso.data.local.despesa

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import br.com.meubolso.domain.model.PagamentoDespesa
import java.time.LocalDate
import java.time.YearMonth

/** Existência da linha = ocorrência PAGA; ausência = PENDENTE. */
@Entity(
    tableName = "pagamentos_despesa",
    primaryKeys = ["despesa_id", "competencia"],
    foreignKeys = [
        ForeignKey(
            entity = DespesaFixaEntity::class,
            parentColumns = ["id"],
            childColumns = ["despesa_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class PagamentoDespesaEntity(
    @ColumnInfo(name = "despesa_id") val despesaId: Long,
    val competencia: YearMonth,
    @ColumnInfo(name = "data_pagamento") val dataPagamento: LocalDate,
)

fun PagamentoDespesaEntity.paraDominio() = PagamentoDespesa(despesaId, competencia, dataPagamento)
