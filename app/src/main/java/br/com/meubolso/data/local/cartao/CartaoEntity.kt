package br.com.meubolso.data.local.cartao

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import br.com.meubolso.domain.model.Cartao
import br.com.meubolso.domain.model.Origem

@Entity(tableName = "cartoes")
data class CartaoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    @ColumnInfo(name = "dia_fechamento") val diaFechamento: Int,
    @ColumnInfo(name = "dia_vencimento") val diaVencimento: Int,
    val fonte: Origem,
)

fun CartaoEntity.paraDominio() = Cartao(
    id = id,
    nome = nome,
    diaFechamento = diaFechamento,
    diaVencimento = diaVencimento,
    fonte = fonte,
)

fun Cartao.paraEntity() = CartaoEntity(
    id = id,
    nome = nome,
    diaFechamento = diaFechamento,
    diaVencimento = diaVencimento,
    fonte = fonte,
)
