package br.com.meubolso.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import br.com.meubolso.data.local.renda.RendaDao
import br.com.meubolso.data.local.renda.RendaEntity

/**
 * Toda alteração de esquema exige incrementar [version] e adicionar uma [Migration]
 * em [MIGRACOES]. Nunca usar fallbackToDestructiveMigration.
 */
@Database(
    entities = [RendaEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Conversores::class)
abstract class MeuBolsoDatabase : RoomDatabase() {

    abstract fun rendaDao(): RendaDao

    companion object {
        const val NOME = "meu_bolso.db"

        val MIGRACOES: Array<Migration> = arrayOf()
    }
}
