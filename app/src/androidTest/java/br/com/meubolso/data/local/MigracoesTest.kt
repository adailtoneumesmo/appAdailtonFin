package br.com.meubolso.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.meubolso.data.local.categoria.CategoriasPadrao
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigracoesTest {

    private val nomeBanco = "teste-migracao.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        MeuBolsoDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migracao1Para2_preservaRendasECriaCategoriasPadrao() {
        helper.createDatabase(nomeBanco, 1).use { db ->
            db.execSQL(
                """
                INSERT INTO rendas (descricao, tipo, valor_centavos, modo_dia, dia, ativo, data_inicio, data_fim)
                VALUES ('Salário Cardway', 'SALARIO', 500000, 'DIA_UTIL', 5, 1, '2026-01-01', NULL)
                """.trimIndent(),
            )
        }

        // Valida o esquema resultante contra app/schemas/.../2.json
        val db = helper.runMigrationsAndValidate(nomeBanco, 2, true, *MeuBolsoDatabase.MIGRACOES)

        db.query("SELECT descricao, valor_centavos FROM rendas").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals("Salário Cardway", cursor.getString(0))
            assertEquals(500000L, cursor.getLong(1))
        }
        db.query("SELECT COUNT(*) FROM categorias WHERE padrao = 1").use { cursor ->
            cursor.moveToFirst()
            assertEquals(CategoriasPadrao.LISTA.size, cursor.getInt(0))
        }
        db.close()
    }
}
