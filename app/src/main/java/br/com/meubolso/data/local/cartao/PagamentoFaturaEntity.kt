package br.com.meubolso.data.local.cartao

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import br.com.meubolso.domain.model.PagamentoFatura
import java.time.LocalDate
import java.time.YearMonth

/** Existência da linha = fatura PAGA; ausência = PENDENTE. [mes] é o mês de referência da fatura. */
@Entity(
    tableName = "pagamentos_fatura",
    primaryKeys = ["cartao_id", "mes"],
    foreignKeys = [
        ForeignKey(
            entity = CartaoEntity::class,
            parentColumns = ["id"],
            childColumns = ["cartao_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class PagamentoFaturaEntity(
    @ColumnInfo(name = "cartao_id") val cartaoId: Long,
    val mes: YearMonth,
    @ColumnInfo(name = "data_pagamento") val dataPagamento: LocalDate,
)

fun PagamentoFaturaEntity.paraDominio() = PagamentoFatura(cartaoId, mes, dataPagamento)
