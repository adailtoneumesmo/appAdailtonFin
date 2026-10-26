package br.com.meubolso.data.remoto

import android.util.Log
import br.com.meubolso.data.local.sincronizacao.SincronizacaoDao
import br.com.meubolso.di.EscopoAplicacao
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

data class EstadoSincronizacao(
    val sincronizando: Boolean = false,
    val ultimaSincronizacao: Instant? = null,
    /** Falha da última tentativa (ex.: sem internet); some na próxima que der certo. */
    val falhou: Boolean = false,
)

/**
 * Mantém o aparelho e a nuvem em dia enquanto houver orçamento ativo:
 * - baixa quando outro membro altera algo (aviso no documento do orçamento);
 * - envia logo depois de cada alteração local, tentando de novo enquanto falhar.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class GerenciadorSincronizacao @Inject constructor(
    private val conta: ContaRepository,
    private val sincronizador: SincronizadorDados,
    private val dao: SincronizacaoDao,
    private val firestore: FirebaseFirestore,
    @EscopoAplicacao private val escopo: CoroutineScope,
) {
    private val _estado = MutableStateFlow(EstadoSincronizacao())
    val estado: StateFlow<EstadoSincronizacao> = _estado.asStateFlow()

    val quantidadePendentes = dao.observarQuantidadePendentes()

    fun iniciar() {
        escopo.launch {
            conta.orcamentoAtualId.collectLatest { orcamentoId ->
                if (orcamentoId == null) return@collectLatest
                coroutineScope {
                    launch {
                        avisosDoOrcamento(orcamentoId).conflate().collect { sincronizar(orcamentoId) }
                    }
                    launch {
                        // Não usa collectLatest: o próprio envio muda a contagem e se cancelaria.
                        // Com conflate, o que mudar durante o envio é visto logo depois dele.
                        dao.observarQuantidadePendentes().conflate().collect { pendentes ->
                            if (pendentes == 0) return@collect
                            // Agrupa alterações seguidas (ex.: vários itens da lista) num envio só.
                            delay(ESPERA_ENVIO_MS)
                            while (!sincronizar(orcamentoId)) delay(ESPERA_NOVA_TENTATIVA_MS)
                        }
                    }
                }
            }
        }
    }

    /** Sincroniza agora; `false` se falhou. */
    suspend fun sincronizar(orcamentoId: String): Boolean {
        _estado.update { it.copy(sincronizando = true) }
        val resultado = runCatching { sincronizador.sincronizar(orcamentoId) }
        resultado.exceptionOrNull()?.let { Log.w(TAG, "Falha ao sincronizar", it) }
        _estado.update {
            it.copy(
                sincronizando = false,
                falhou = resultado.isFailure,
                ultimaSincronizacao = if (resultado.isSuccess) Instant.now() else it.ultimaSincronizacao,
            )
        }
        return resultado.isSuccess
    }

    /** Emite ao conectar e a cada alteração do orçamento (inclusive vindas de outro aparelho). */
    private fun avisosDoOrcamento(orcamentoId: String) = callbackFlow {
        val registro = Firestore.orcamento(firestore, orcamentoId).addSnapshotListener { snap, _ ->
            if (snap != null && !snap.metadata.hasPendingWrites()) trySend(Unit)
        }
        awaitClose { registro.remove() }
    }

    private companion object {
        const val TAG = "Sincronizacao"
        const val ESPERA_ENVIO_MS = 2_000L
        const val ESPERA_NOVA_TENTATIVA_MS = 30_000L
    }
}
