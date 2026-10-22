package br.com.meubolso.data.local

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.YearMonth

/** Datas gravadas como texto ISO (yyyy-MM-dd / yyyy-MM): legível e ordenável no SQLite. */
class Conversores {
    @TypeConverter
    fun deLocalDate(data: LocalDate?): String? = data?.toString()

    @TypeConverter
    fun paraLocalDate(valor: String?): LocalDate? = valor?.let(LocalDate::parse)

    @TypeConverter
    fun deYearMonth(mes: YearMonth?): String? = mes?.toString()

    @TypeConverter
    fun paraYearMonth(valor: String?): YearMonth? = valor?.let(YearMonth::parse)
}
