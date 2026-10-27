package br.com.meubolso.data.local.objetivo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import br.com.meubolso.domain.model.Objetivo
import br.com.meubolso.domain.model.PosicaoInvestimentos
import java.time.LocalDate

@Entity(tableName = "objetivos")
data class ObjetivoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    @ColumnInfo(name = "valor_alvo_centavos") val valorAlvoCentavos: Long,
    @ColumnInfo(name = "data_alvo") val dataAlvo: LocalDate?,
)

/** Histórico do total investido; a linha com a [dataReferencia] mais recente é a atual. */
@Entity(tableName = "posicoes_investimento")
data class PosicaoInvestimentoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "valor_centavos") val valorCentavos: Long,
    @ColumnInfo(name = "data_referencia") val dataReferencia: LocalDate,
)

fun ObjetivoEntity.paraDominio() = Objetivo(id = id, nome = nome, valorAlvoCentavos = valorAlvoCentavos, dataAlvo = dataAlvo)

fun Objetivo.paraEntity() = ObjetivoEntity(id = id, nome = nome, valorAlvoCentavos = valorAlvoCentavos, dataAlvo = dataAlvo)

fun PosicaoInvestimentoEntity.paraDominio() =
    PosicaoInvestimentos(id = id, valorCentavos = valorCentavos, dataReferencia = dataReferencia)
