package br.com.meubolso.data.local.sincronizacao

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.sqlite.db.SupportSQLiteDatabase
import br.com.meubolso.data.local.MeuBolsoDatabase
import br.com.meubolso.data.local.categoria.CategoriasPadrao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Uma linha como está na nuvem: colunas da tabela, com chaves estrangeiras trocadas por UUID. */
data class DocumentoRemoto(
    val uuid: String,
    val dados: Map<String, Any?>,
    val atualizadoEm: Long,
    val excluido: Boolean,
)

/**
 * Lado local da sincronização, em SQL direto: funciona igual para todas as tabelas, sem
 * depender das entidades. Colunas e chaves estrangeiras vêm do próprio esquema (PRAGMA),
 * então tabelas e colunas novas sincronizam sem mudar este código.
 */
@Singleton
class ArmazemSincronizacao @Inject constructor(
    private val database: MeuBolsoDatabase,
) {
    private val db: SupportSQLiteDatabase get() = database.openHelper.writableDatabase

    /** Coluna → tabela referenciada. */
    private val chavesEstrangeiras = mutableMapOf<String, Map<String, String>>()
    private val colunas = mutableMapOf<String, Set<String>>()

    /**
     * Chave única "natural" das tabelas que têm uma. Dois aparelhos podem criar a mesma linha
     * (ex.: categoria "Pets" nos dois): na chegada, a linha local é ligada à da nuvem.
     */
    private val chavesNaturais = mapOf(
        "categorias" to listOf("nome"),
        "pagamentos_despesa" to listOf("despesa_id", "competencia"),
    )

    /** Dados da linha para enviar; `null` se ela não existe mais. */
    suspend fun lerParaEnvio(registro: RegistroSincronizacaoEntity): Map<String, Any?>? = withContext(Dispatchers.IO) {
        val idLocal = registro.idLocal ?: return@withContext null
        val fks = chavesEstrangeiras(registro.tabela)
        db.query("SELECT * FROM ${registro.tabela} WHERE rowid = ?", arrayOf(idLocal)).use { c ->
            if (!c.moveToFirst()) return@withContext null
            buildMap {
                for (i in 0 until c.columnCount) {
                    val nome = c.getColumnName(i)
                    if (nome == "id") continue
                    val valor = c.valor(i)
                    val pai = fks[nome]
                    put(nome, if (pai != null && valor is Long) uuidDe(pai, valor) else valor)
                }
            }
        }
    }

    /**
     * Grava o que veio da nuvem, com os gatilhos desligados. Em conflito vence a alteração
     * mais recente: se a local for mais nova e ainda não subiu, a remota é ignorada.
     */
    suspend fun aplicar(tabela: String, documentos: List<DocumentoRemoto>) = withContext(Dispatchers.IO) {
        if (documentos.isEmpty()) return@withContext
        database.runInTransaction {
            db.execSQL("INSERT OR REPLACE INTO sync_controle (id, aplicando) VALUES (1, 1)")
            try {
                documentos.forEach { doc ->
                    try {
                        aplicar(tabela, doc)
                    } catch (e: SQLiteConstraintException) {
                        // Ex.: pai excluído aqui ou categoria ainda em uso; a linha fica de fora.
                        Log.w(TAG, "Ignorado ${doc.uuid} em $tabela", e)
                    }
                }
            } finally {
                db.execSQL("UPDATE sync_controle SET aplicando = 0 WHERE id = 1")
            }
        }
    }

    private fun aplicar(tabela: String, doc: DocumentoRemoto) {
        val registro = registro(doc.uuid)
        if (registro != null) {
            if (!registro.pendente && registro.atualizadoEm == doc.atualizadoEm) return
            if (registro.pendente && registro.atualizadoEm >= doc.atualizadoEm) return
        }

        if (doc.excluido) {
            registro?.idLocal?.let { db.delete(tabela, "rowid = ?", arrayOf(it)) }
            db.delete("sync_registro", "uuid = ?", arrayOf(doc.uuid))
            return
        }

        val valores = paraContentValues(tabela, doc.dados)
        val idLocal = registro?.idLocal
        if (idLocal != null) {
            db.update(tabela, SQLiteDatabase.CONFLICT_ABORT, valores, "rowid = ?", arrayOf(idLocal))
            salvarRegistro(doc, tabela, idLocal)
            return
        }
        val novoId = try {
            db.insert(tabela, SQLiteDatabase.CONFLICT_ABORT, valores)
        } catch (e: SQLiteConstraintException) {
            val existente = buscarPorChaveNatural(tabela, valores) ?: throw e
            ligarLinhaExistente(tabela, existente)
            db.update(tabela, SQLiteDatabase.CONFLICT_ABORT, valores, "rowid = ?", arrayOf(existente))
            existente
        }
        salvarRegistro(doc, tabela, novoId)
    }

    /** A linha local passa a ter o UUID da nuvem; o UUID antigo vira exclusão pendente (caso já tenha subido). */
    private fun ligarLinhaExistente(tabela: String, idLocal: Long) {
        val antigo = db.query(
            "SELECT uuid FROM sync_registro WHERE tabela = ? AND id_local = ?",
            arrayOf<Any>(tabela, idLocal),
        ).use { if (it.moveToFirst()) it.getString(0) else null } ?: return
        db.execSQL(
            "UPDATE sync_registro SET id_local = NULL, excluido = 1, pendente = 1 WHERE uuid = ?",
            arrayOf(antigo),
        )
    }

    private fun salvarRegistro(doc: DocumentoRemoto, tabela: String, idLocal: Long) {
        db.insert(
            "sync_registro",
            SQLiteDatabase.CONFLICT_REPLACE,
            ContentValues().apply {
                put("uuid", doc.uuid)
                put("tabela", tabela)
                put("id_local", idLocal)
                put("atualizado_em", doc.atualizadoEm)
                put("pendente", 0)
                put("excluido", 0)
            },
        )
    }

    private fun buscarPorChaveNatural(tabela: String, valores: ContentValues): Long? {
        val chave = chavesNaturais[tabela] ?: return null
        val args = chave.map { valores.get(it) ?: return null }
        return db.query(
            "SELECT rowid FROM $tabela WHERE ${chave.joinToString(" AND ") { "$it = ?" }}",
            args.toTypedArray(),
        ).use { if (it.moveToFirst()) it.getLong(0) else null }
    }

    private fun paraContentValues(tabela: String, dados: Map<String, Any?>): ContentValues {
        val fks = chavesEstrangeiras(tabela)
        val existentes = colunas(tabela)
        return ContentValues().apply {
            dados.forEach { (coluna, valor) ->
                if (coluna == "id" || coluna !in existentes) return@forEach
                val pai = fks[coluna]
                val convertido = if (pai != null && valor is String) idLocalDe(pai, valor) else valor
                when (convertido) {
                    null -> putNull(coluna)
                    is Long -> put(coluna, convertido)
                    is Int -> put(coluna, convertido)
                    is Double -> put(coluna, convertido)
                    is Boolean -> put(coluna, if (convertido) 1 else 0)
                    else -> put(coluna, convertido.toString())
                }
            }
        }
    }

    private fun registro(uuid: String): RegistroSincronizacaoEntity? =
        db.query("SELECT uuid, tabela, id_local, atualizado_em, pendente, excluido FROM sync_registro WHERE uuid = ?", arrayOf(uuid))
            .use { c ->
                if (!c.moveToFirst()) return null
                RegistroSincronizacaoEntity(
                    uuid = c.getString(0),
                    tabela = c.getString(1),
                    idLocal = if (c.isNull(2)) null else c.getLong(2),
                    atualizadoEm = c.getLong(3),
                    pendente = c.getInt(4) != 0,
                    excluido = c.getInt(5) != 0,
                )
            }

    private fun uuidDe(tabela: String, idLocal: Long): String? =
        db.query("SELECT uuid FROM sync_registro WHERE tabela = ? AND id_local = ?", arrayOf<Any>(tabela, idLocal))
            .use { if (it.moveToFirst()) it.getString(0) else null }

    private fun idLocalDe(tabela: String, uuid: String): Long? =
        db.query("SELECT id_local FROM sync_registro WHERE tabela = ? AND uuid = ?", arrayOf(tabela, uuid))
            .use { if (it.moveToFirst() && !it.isNull(0)) it.getLong(0) else null }

    private fun chavesEstrangeiras(tabela: String): Map<String, String> = chavesEstrangeiras.getOrPut(tabela) {
        db.query("PRAGMA foreign_key_list($tabela)").use { c ->
            val colTabela = c.getColumnIndexOrThrow("table")
            val colDe = c.getColumnIndexOrThrow("from")
            buildMap { while (c.moveToNext()) put(c.getString(colDe), c.getString(colTabela)) }
        }
    }

    private fun colunas(tabela: String): Set<String> = colunas.getOrPut(tabela) {
        db.query("PRAGMA table_info($tabela)").use { c ->
            val colNome = c.getColumnIndexOrThrow("name")
            buildSet { while (c.moveToNext()) add(c.getString(colNome)) }
        }
    }

    /**
     * Ao sair da conta os dados ficam só na nuvem: o aparelho volta ao estado de instalação
     * nova (só as categorias padrão), pronto para outra conta.
     */
    suspend fun limparTudo() = withContext(Dispatchers.IO) {
        database.clearAllTables()
        database.runInTransaction {
            db.execSQL("DELETE FROM sync_registro")
            db.execSQL("DELETE FROM sync_cursor")
            CategoriasPadrao.inserir(db)
            GatilhosSincronizacao.fixarUuidsCategoriasPadrao(db)
        }
    }

    private fun Cursor.valor(i: Int): Any? = when (getType(i)) {
        Cursor.FIELD_TYPE_NULL -> null
        Cursor.FIELD_TYPE_INTEGER -> getLong(i)
        Cursor.FIELD_TYPE_FLOAT -> getDouble(i)
        else -> getString(i)
    }

    private companion object {
        const val TAG = "Sincronizacao"
    }
}
