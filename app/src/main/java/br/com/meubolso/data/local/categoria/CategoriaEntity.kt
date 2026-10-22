package br.com.meubolso.data.local.categoria

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import br.com.meubolso.domain.model.Categoria

@Entity(
    tableName = "categorias",
    indices = [Index(value = ["nome"], unique = true)],
)
data class CategoriaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val cor: Int,
    val padrao: Boolean,
)

fun CategoriaEntity.paraDominio() = Categoria(id = id, nome = nome, cor = cor, padrao = padrao)

fun Categoria.paraEntity() = CategoriaEntity(id = id, nome = nome, cor = cor, padrao = padrao)
