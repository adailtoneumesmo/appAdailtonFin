package br.com.meubolso.data.local.sincronizacao

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Identidade global de cada linha sincronizada. Mantida pelos gatilhos de [GatilhosSincronizacao]:
 * as telas e DAOs gravam normalmente e a linha daqui é criada/atualizada sozinha.
 *
 * Na exclusão, [idLocal] vira `null` e [excluido] `true` até a exclusão subir para a nuvem.
 */
@Entity(
    tableName = "sync_registro",
    indices = [Index(value = ["tabela", "id_local"], unique = true), Index("pendente")],
)
data class RegistroSincronizacaoEntity(
    @PrimaryKey val uuid: String,
    val tabela: String,
    /** rowid da linha na [tabela]. */
    @ColumnInfo(name = "id_local") val idLocal: Long?,
    /** Epoch millis da última alteração; decide conflitos (vence a mais recente). */
    @ColumnInfo(name = "atualizado_em") val atualizadoEm: Long,
    /** Alterado aqui e ainda não enviado. */
    val pendente: Boolean,
    val excluido: Boolean,
)

/** Até onde cada coleção já foi baixada (horário do servidor, epoch millis). */
@Entity(tableName = "sync_cursor")
data class CursorSincronizacaoEntity(
    @PrimaryKey val colecao: String,
    @ColumnInfo(name = "servidor_millis") val servidorMillis: Long,
)

/**
 * Linha única: com [aplicando] = 1 os gatilhos ficam desligados, para que gravar dados
 * vindos da nuvem não os marque como pendentes de envio de novo.
 */
@Entity(tableName = "sync_controle")
data class ControleSincronizacaoEntity(
    @PrimaryKey val id: Int = 1,
    val aplicando: Boolean,
)
