package br.com.meubolso.data.local.renda

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import br.com.meubolso.domain.model.ModoDiaRecebimento
import br.com.meubolso.domain.model.Renda
import br.com.meubolso.domain.model.TipoRenda
import java.time.LocalDate

@Entity(tableName = "rendas")
data class RendaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val descricao: String,
    val tipo: TipoRenda,
    @ColumnInfo(name = "valor_centavos") val valorCentavos: Long,
    @ColumnInfo(name = "modo_dia") val modoDia: ModoDiaRecebimento,
    val dia: Int,
    val ativo: Boolean,
    @ColumnInfo(name = "data_inicio") val dataInicio: LocalDate,
    @ColumnInfo(name = "data_fim") val dataFim: LocalDate?,
)

fun RendaEntity.paraDominio() = Renda(
    id = id,
    descricao = descricao,
    tipo = tipo,
    valorCentavos = valorCentavos,
    modoDia = modoDia,
    dia = dia,
    ativo = ativo,
    dataInicio = dataInicio,
    dataFim = dataFim,
)

fun Renda.paraEntity() = RendaEntity(
    id = id,
    descricao = descricao,
    tipo = tipo,
    valorCentavos = valorCentavos,
    modoDia = modoDia,
    dia = dia,
    ativo = ativo,
    dataInicio = dataInicio,
    dataFim = dataFim,
)
