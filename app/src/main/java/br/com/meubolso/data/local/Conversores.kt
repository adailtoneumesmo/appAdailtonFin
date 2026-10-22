package br.com.meubolso.data.local

import androidx.room.TypeConverter
import java.time.LocalDate

/** Datas gravadas como texto ISO (yyyy-MM-dd): legível e ordenável no SQLite. */
class Conversores {
    @TypeConverter
    fun deLocalDate(data: LocalDate?): String? = data?.toString()

    @TypeConverter
    fun paraLocalDate(valor: String?): LocalDate? = valor?.let(LocalDate::parse)
}
