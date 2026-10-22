package br.com.meubolso.data.local.categoria

import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Categorias criadas na instalação (onCreate) e na migração 1→2.
 * Os nomes ficam no banco porque são dados editáveis pelo usuário, não textos de interface.
 */
object CategoriasPadrao {

    val LISTA: List<Pair<String, Int>> = listOf(
        "Moradia" to 0xFF127A45.toInt(),
        "Alimentação" to 0xFFE8A317.toInt(),
        "Transporte" to 0xFF2F6FB0.toInt(),
        "Saúde" to 0xFFC2477A.toInt(),
        "Educação" to 0xFF7B5EA7.toInt(),
        "Lazer" to 0xFFE07B39.toInt(),
        "Assinaturas" to 0xFF2BA3A3.toInt(),
        "Cartão" to 0xFF0E3B24.toInt(),
        "Outros" to 0xFF7D8A80.toInt(),
    )

    fun inserir(db: SupportSQLiteDatabase) {
        LISTA.forEach { (nome, cor) ->
            db.execSQL(
                "INSERT OR IGNORE INTO categorias (nome, cor, padrao) VALUES (?, ?, 1)",
                arrayOf<Any>(nome, cor),
            )
        }
    }
}
