package br.com.meubolso.data.remoto

import br.com.meubolso.data.local.sincronizacao.ArmazemSincronizacao
import br.com.meubolso.data.local.sincronizacao.CursorSincronizacaoEntity
import br.com.meubolso.data.local.sincronizacao.DocumentoRemoto
import br.com.meubolso.data.local.sincronizacao.GatilhosSincronizacao
import br.com.meubolso.data.local.sincronizacao.SincronizacaoDao
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Troca dados entre o Room e o Firestore. Cada tabela vira a coleção
 * `orcamentos/{id}/{tabela}`, com um documento por linha (id = UUID do `sync_registro`).
 *
 * Sempre baixa antes de enviar: assim uma linha criada nos dois aparelhos (ex.: mesma
 * categoria) é ligada à da nuvem antes de subir duplicada.
 */
@Singleton
class SincronizadorDados @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val armazem: ArmazemSincronizacao,
    private val dao: SincronizacaoDao,
) {
    private val mutex = Mutex()

    suspend fun sincronizar(orcamentoId: String) = mutex.withLock {
        baixar(orcamentoId)
        enviar(orcamentoId)
    }

    private suspend fun baixar(orcamentoId: String) {
        GatilhosSincronizacao.TABELAS.forEach { tabela ->
            val cursor = dao.cursor(tabela) ?: 0L
            // >= e não >: documentos no mesmo milissegundo do cursor não se perdem (aplicar é idempotente).
            val documentos = Firestore.colecao(firestore, orcamentoId, tabela)
                .whereGreaterThanOrEqualTo(Firestore.CAMPO_SINCRONIZADO_EM, Timestamp(Date(cursor)))
                .orderBy(Firestore.CAMPO_SINCRONIZADO_EM, Query.Direction.ASCENDING)
                .get(Source.SERVER)
                .await()
                .documents
            armazem.aplicar(tabela, documentos.mapNotNull { it.paraDocumentoRemoto() })
            documentos.mapNotNull { it.getTimestamp(Firestore.CAMPO_SINCRONIZADO_EM)?.toDate()?.time }.maxOrNull()?.let {
                dao.salvarCursor(CursorSincronizacaoEntity(tabela, it))
            }
        }
    }

    private suspend fun enviar(orcamentoId: String) {
        dao.buscarPendentes().chunked(LIMITE_LOTE).forEach { lote ->
            val batch = firestore.batch()
            lote.forEach { registro ->
                val doc = Firestore.colecao(firestore, orcamentoId, registro.tabela).document(registro.uuid)
                val dados = if (registro.excluido) emptyMap() else armazem.lerParaEnvio(registro) ?: return@forEach
                batch.set(
                    doc,
                    dados + mapOf(
                        Firestore.CAMPO_ATUALIZADO_EM to registro.atualizadoEm,
                        Firestore.CAMPO_EXCLUIDO to registro.excluido,
                        Firestore.CAMPO_SINCRONIZADO_EM to FieldValue.serverTimestamp(),
                    ),
                )
            }
            // Avisa os outros aparelhos (que observam o orçamento) que há novidade.
            batch.set(
                Firestore.orcamento(firestore, orcamentoId),
                mapOf(Firestore.CAMPO_ALTERADO_EM to FieldValue.serverTimestamp()),
                SetOptions.merge(),
            )
            batch.commit().await()
            lote.forEach { registro ->
                if (registro.excluido) {
                    dao.removerExclusaoEnviada(registro.uuid, registro.atualizadoEm)
                } else {
                    dao.marcarEnviado(registro.uuid, registro.atualizadoEm)
                }
            }
        }
    }

    private fun DocumentSnapshot.paraDocumentoRemoto(): DocumentoRemoto? {
        val dados = data ?: return null
        return DocumentoRemoto(
            uuid = id,
            dados = dados.filterKeys { !it.startsWith(Firestore.PREFIXO_META) },
            atualizadoEm = getLong(Firestore.CAMPO_ATUALIZADO_EM) ?: 0L,
            excluido = getBoolean(Firestore.CAMPO_EXCLUIDO) ?: false,
        )
    }

    private companion object {
        /** O Firestore aceita até 500 operações por lote; sobra espaço para o aviso do orçamento. */
        const val LIMITE_LOTE = 400
    }
}
