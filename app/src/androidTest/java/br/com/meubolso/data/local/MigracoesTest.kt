package br.com.meubolso.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.meubolso.data.local.categoria.CategoriasPadrao
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun migracao2Para3_preservaDespesasECriaTabelasDeCartao() {
        helper.createDatabase(nomeBanco, 2).use { db ->
            CategoriasPadrao.inserir(db)
            db.execSQL(
                """
                INSERT INTO despesas_fixas (descricao, categoria_id, valor_centavos, dia_vencimento, forma_pagamento,
                    vinculo, recorrencia, total_parcelas, mes_inicio, ativa)
                VALUES ('Aluguel', 1, 150000, 5, 'BOLETO', 'SALARIO', 'MENSAL', NULL, '2026-01', 1)
                """.trimIndent(),
            )
        }

        // Valida o esquema resultante contra app/schemas/.../3.json
        val db = helper.runMigrationsAndValidate(nomeBanco, 3, true, *MeuBolsoDatabase.MIGRACOES)

        db.query("SELECT descricao FROM despesas_fixas").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals("Aluguel", cursor.getString(0))
        }
        db.execSQL("INSERT INTO cartoes (nome, dia_fechamento, dia_vencimento, fonte) VALUES ('Nubank', 10, 17, 'VALE')")
        db.execSQL(
            """
            INSERT INTO compras_cartao (cartao_id, descricao, categoria_id, valor_centavos, data_compra, modalidade,
                total_parcelas, mes_primeira_fatura, mes_ultima_fatura)
            VALUES (1, 'Notebook', 1, 500000, '2026-03-05', 'PARCELADO', 5, '2026-03', NULL)
            """.trimIndent(),
        )
        db.query("SELECT COUNT(*) FROM compras_cartao").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
        }
        db.close()
    }

    @Test
    fun migracao3Para4_preservaRendasECriaListasDeCompra() {
        helper.createDatabase(nomeBanco, 3).use { db ->
            CategoriasPadrao.inserir(db)
            db.execSQL(
                """
                INSERT INTO rendas (descricao, tipo, valor_centavos, modo_dia, dia, ativo, data_inicio, data_fim)
                VALUES ('Salário Cardway', 'SALARIO', 500000, 'DIA_UTIL', 5, 1, '2026-01-01', NULL)
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO despesas_fixas (descricao, categoria_id, valor_centavos, dia_vencimento, forma_pagamento,
                    vinculo, recorrencia, total_parcelas, mes_inicio, ativa)
                VALUES ('Aluguel', 1, 150000, 5, 'BOLETO', 'SALARIO', 'MENSAL', NULL, '2026-01', 1)
                """.trimIndent(),
            )
        }

        // Valida o esquema resultante contra app/schemas/.../4.json
        val db = helper.runMigrationsAndValidate(nomeBanco, 4, true, *MeuBolsoDatabase.MIGRACOES)

        db.query("SELECT descricao, valor_centavos, forma_calculo_vale FROM rendas").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals("Salário Cardway", cursor.getString(0))
            assertEquals(500000L, cursor.getLong(1))
            assertTrue(cursor.isNull(2))
        }
        db.query("SELECT COUNT(*) FROM despesas_fixas").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
        }

        db.execSQL(
            """
            INSERT INTO rendas (descricao, tipo, valor_centavos, modo_dia, dia, ativo, data_inicio, data_fim, forma_calculo_vale)
            VALUES ('VR', 'VALE_ALIMENTACAO', 2000, 'DIA_FIXO', 25, 1, '2026-01-01', NULL, 'VALOR_POR_DIA')
            """.trimIndent(),
        )
        db.execSQL("INSERT INTO listas_compra (descricao, data, limite_centavos, despesa_id) VALUES ('Mercado', '2026-10-26', 38000, 1)")
        db.execSQL(
            """
            INSERT INTO itens_lista_compra (lista_id, nome, valor_centavos, quantidade_milesimos, no_carrinho, ordem)
            VALUES (1, 'Banana', 899, 1500, 0, 0)
            """.trimIndent(),
        )
        // Excluir a despesa gerada desfaz o vínculo (SET NULL) sem apagar a lista.
        db.execSQL("PRAGMA foreign_keys = ON")
        db.execSQL("DELETE FROM despesas_fixas WHERE id = 1")
        db.query("SELECT despesa_id FROM listas_compra").use { cursor ->
            cursor.moveToFirst()
            assertTrue(cursor.isNull(0))
        }
        db.close()
    }
}
