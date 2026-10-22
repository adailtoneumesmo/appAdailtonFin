package br.com.meubolso.data.repository

import br.com.meubolso.data.local.categoria.CategoriaDao
import br.com.meubolso.data.local.categoria.paraDominio
import br.com.meubolso.data.local.categoria.paraEntity
import br.com.meubolso.domain.model.Categoria
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoriaRepository @Inject constructor(
    private val dao: CategoriaDao,
) {
    fun observarCategorias(): Flow<List<Categoria>> =
        dao.observarTodas().map { lista -> lista.map { it.paraDominio() } }

    suspend fun salvar(categoria: Categoria) = dao.salvar(categoria.paraEntity())

    /** Exclui se não houver despesas vinculadas; retorna quantas despesas impediram a exclusão. */
    suspend fun excluir(id: Long): Int {
        val emUso = dao.contarDespesas(id)
        if (emUso == 0) dao.excluir(id)
        return emUso
    }
}
