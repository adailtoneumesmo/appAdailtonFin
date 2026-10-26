package br.com.meubolso.data.local.notificacao

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import br.com.meubolso.domain.model.NotificacaoBanco
import br.com.meubolso.domain.model.StatusNotificacao
import br.com.meubolso.domain.model.TipoTransacao
import java.time.Instant

/**
 * [chave] é única: o banco pode republicar a mesma notificação (atualização, reabertura do
 * app) e ela não deve virar duas pendências.
 */
@Entity(tableName = "notificacoes_banco", indices = [Index("chave", unique = true), Index("status")])
data class NotificacaoBancoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chave: String,
    val pacote: String,
    val banco: String,
    val titulo: String,
    val texto: String,
    @ColumnInfo(name = "recebida_em_millis") val recebidaEmMillis: Long,
    @ColumnInfo(name = "valor_centavos") val valorCentavos: Long?,
    @ColumnInfo(name = "descricao_sugerida") val descricaoSugerida: String?,
    val tipo: TipoTransacao,
    val vale: Boolean,
    val status: StatusNotificacao,
)

fun NotificacaoBancoEntity.paraDominio() = NotificacaoBanco(
    id = id,
    pacote = pacote,
    banco = banco,
    titulo = titulo,
    texto = texto,
    recebidaEm = Instant.ofEpochMilli(recebidaEmMillis),
    valorCentavos = valorCentavos,
    descricaoSugerida = descricaoSugerida,
    tipo = tipo,
    vale = vale,
    status = status,
)

fun NotificacaoBanco.paraEntity(chave: String) = NotificacaoBancoEntity(
    id = id,
    chave = chave,
    pacote = pacote,
    banco = banco,
    titulo = titulo,
    texto = texto,
    recebidaEmMillis = recebidaEm.toEpochMilli(),
    valorCentavos = valorCentavos,
    descricaoSugerida = descricaoSugerida,
    tipo = tipo,
    vale = vale,
    status = status,
)
