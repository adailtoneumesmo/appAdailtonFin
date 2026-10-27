package br.com.meubolso.data.repository

import br.com.meubolso.data.local.objetivo.ObjetivoDao
import br.com.meubolso.data.local.objetivo.PosicaoInvestimentoEntity
import br.com.meubolso.data.local.objetivo.paraDominio
import br.com.meubolso.data.local.objetivo.paraEntity
import br.com.meubolso.domain.model.Objetivo
import br.com.meubolso.domain.model.PosicaoInvestimentos
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ObjetivoRepository @Inject constructor(
    private val dao: ObjetivoDao,
) {
    fun observarObjetivos(): Flow<List<Objetivo>> =
        dao.observarTodos().map { lista -> lista.map { it.paraDominio() } }

    fun observarPosicaoAtual(): Flow<PosicaoInvestimentos?> = dao.observarPosicaoAtual().map { it?.paraDominio() }

    suspend fun buscar(id: Long): Objetivo? = dao.buscarPorId(id)?.paraDominio()

    suspend fun salvar(objetivo: Objetivo) = dao.salvar(objetivo.paraEntity())

    suspend fun excluir(id: Long) = dao.excluir(id)

    /** Uma posição por dia: corrigir o valor no mesmo dia substitui o anterior em vez de acumular histórico. */
    suspend fun registrarPosicao(valorCentavos: Long, data: LocalDate) {
        val existente = dao.buscarPosicaoDoDia(data)
        dao.salvarPosicao(PosicaoInvestimentoEntity(id = existente?.id ?: 0, valorCentavos = valorCentavos, dataReferencia = data))
    }
}
