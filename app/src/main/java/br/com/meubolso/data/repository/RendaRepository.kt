package br.com.meubolso.data.repository

import br.com.meubolso.data.local.renda.RendaDao
import br.com.meubolso.data.local.renda.paraDominio
import br.com.meubolso.data.local.renda.paraEntity
import br.com.meubolso.domain.model.Renda
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RendaRepository @Inject constructor(
    private val dao: RendaDao,
) {
    fun observarRendas(): Flow<List<Renda>> =
        dao.observarTodas().map { lista -> lista.map { it.paraDominio() } }

    suspend fun buscar(id: Long): Renda? = dao.buscarPorId(id)?.paraDominio()

    suspend fun salvar(renda: Renda) = dao.salvar(renda.paraEntity())

    suspend fun excluir(id: Long) = dao.excluir(id)
}
