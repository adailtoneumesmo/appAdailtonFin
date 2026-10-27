package br.com.meubolso.data.local.sincronizacao

import androidx.sqlite.db.SupportSQLiteDatabase
import br.com.meubolso.data.local.categoria.CategoriasPadrao

/**
 * Gatilhos do SQLite que registram em `sync_registro` toda inclusão, edição e exclusão das
 * tabelas sincronizadas. Ficam fora do Room (que não gerencia gatilhos) e são criados na
 * instalação e na migração 5→6.
 *
 * Só SQL compatível com o SQLite do minSdk 26 (sem UPSERT): INSERT OR IGNORE + UPDATE.
 */
object GatilhosSincronizacao {

    /**
     * Tabelas enviadas para a nuvem, na ordem em que são aplicadas ao baixar: pais antes dos
     * filhos, para as chaves estrangeiras existirem. `notificacoes_banco` fica de fora de
     * propósito: o texto bruto das notificações não sai do aparelho.
     */
    val TABELAS = listOf(
        "categorias",
        "cartoes",
        "rendas",
        "despesas_fixas",
        "pagamentos_despesa",
        "compras_cartao",
        "listas_compra",
        "itens_lista_compra",
        "objetivos",
        "posicoes_investimento",
    )

    private const val AGORA = "CAST((julianday('now') - 2440587.5) * 86400000 AS INTEGER)"
    private const val NOVO_UUID = "lower(hex(randomblob(16)))"
    private const val ATIVOS = "COALESCE((SELECT aplicando FROM sync_controle WHERE id = 1), 0) = 0"

    fun instalar(db: SupportSQLiteDatabase) {
        tabelasExistentes(db).forEach { tabela ->
            val marcar = """
                INSERT OR IGNORE INTO sync_registro (uuid, tabela, id_local, atualizado_em, pendente, excluido)
                    VALUES ($NOVO_UUID, '$tabela', NEW.rowid, $AGORA, 1, 0);
                UPDATE sync_registro SET atualizado_em = $AGORA, pendente = 1
                    WHERE tabela = '$tabela' AND id_local = NEW.rowid;
            """
            db.execSQL("CREATE TRIGGER IF NOT EXISTS sync_${tabela}_ins AFTER INSERT ON $tabela WHEN $ATIVOS BEGIN $marcar END")
            db.execSQL("CREATE TRIGGER IF NOT EXISTS sync_${tabela}_upd AFTER UPDATE ON $tabela WHEN $ATIVOS BEGIN $marcar END")
            // Exclusões em cascata também disparam, então os filhos viram exclusões pendentes junto.
            db.execSQL(
                """
                CREATE TRIGGER IF NOT EXISTS sync_${tabela}_del AFTER DELETE ON $tabela WHEN $ATIVOS BEGIN
                    UPDATE sync_registro SET id_local = NULL, excluido = 1, pendente = 1, atualizado_em = $AGORA
                        WHERE tabela = '$tabela' AND id_local = OLD.rowid;
                END
                """.trimIndent(),
            )
        }
    }

    /** Registra as linhas que já existiam antes dos gatilhos, como pendentes de envio. */
    fun registrarExistentes(db: SupportSQLiteDatabase) {
        tabelasExistentes(db).forEach { tabela ->
            db.execSQL(
                """
                INSERT OR IGNORE INTO sync_registro (uuid, tabela, id_local, atualizado_em, pendente, excluido)
                SELECT $NOVO_UUID, '$tabela', rowid, $AGORA, 1, 0 FROM $tabela
                """.trimIndent(),
            )
        }
        fixarUuidsCategoriasPadrao(db)
    }

    /**
     * Numa migração em sequência (ex.: 5→6→7) as tabelas de versões futuras ainda não existem;
     * os gatilhos delas são criados pela migração que as cria.
     */
    private fun tabelasExistentes(db: SupportSQLiteDatabase): List<String> {
        val existentes = db.query("SELECT name FROM sqlite_master WHERE type = 'table'").use { c ->
            buildSet { while (c.moveToNext()) add(c.getString(0)) }
        }
        return TABELAS.filter { it in existentes }
    }

    /**
     * As categorias padrão nascem em todo aparelho; com o mesmo UUID em todos, elas se
     * fundem na nuvem em vez de duplicar quando alguém entra num orçamento compartilhado.
     */
    fun fixarUuidsCategoriasPadrao(db: SupportSQLiteDatabase) {
        CategoriasPadrao.LISTA.forEach { (nome, _) ->
            db.execSQL(
                """
                UPDATE OR IGNORE sync_registro SET uuid = ?
                WHERE tabela = 'categorias' AND id_local = (SELECT id FROM categorias WHERE nome = ? AND padrao = 1)
                """.trimIndent(),
                arrayOf<Any>(uuidCategoriaPadrao(nome), nome),
            )
        }
    }

    fun uuidCategoriaPadrao(nome: String) = "categoria-padrao-${nome.lowercase()}"
}
