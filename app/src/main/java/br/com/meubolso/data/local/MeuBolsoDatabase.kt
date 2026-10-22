package br.com.meubolso.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import br.com.meubolso.data.local.categoria.CategoriaDao
import br.com.meubolso.data.local.categoria.CategoriaEntity
import br.com.meubolso.data.local.categoria.CategoriasPadrao
import br.com.meubolso.data.local.despesa.DespesaDao
import br.com.meubolso.data.local.despesa.DespesaFixaEntity
import br.com.meubolso.data.local.despesa.PagamentoDespesaEntity
import br.com.meubolso.data.local.renda.RendaDao
import br.com.meubolso.data.local.renda.RendaEntity

/**
 * Toda alteração de esquema exige incrementar [version] e adicionar uma migração
 * (automática ou em [MIGRACOES]). Nunca usar fallbackToDestructiveMigration.
 *
 * Histórico:
 * - v1: rendas
 * - v2: categorias (com as padrão), despesas_fixas e pagamentos_despesa
 */
@Database(
    entities = [
        RendaEntity::class,
        CategoriaEntity::class,
        DespesaFixaEntity::class,
        PagamentoDespesaEntity::class,
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2, spec = MeuBolsoDatabase.Migracao1Para2::class),
    ],
)
@TypeConverters(Conversores::class)
abstract class MeuBolsoDatabase : RoomDatabase() {

    abstract fun rendaDao(): RendaDao

    abstract fun categoriaDao(): CategoriaDao

    abstract fun despesaDao(): DespesaDao

    /** Tabelas novas são criadas pelo Room; aqui só populamos as categorias padrão. */
    class Migracao1Para2 : AutoMigrationSpec {
        override fun onPostMigrate(db: SupportSQLiteDatabase) = CategoriasPadrao.inserir(db)
    }

    /** Instalação nova: o banco já nasce na versão atual, sem passar pelas migrações. */
    object AoCriar : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) = CategoriasPadrao.inserir(db)
    }

    companion object {
        const val NOME = "meu_bolso.db"

        val MIGRACOES: Array<Migration> = arrayOf()
    }
}
