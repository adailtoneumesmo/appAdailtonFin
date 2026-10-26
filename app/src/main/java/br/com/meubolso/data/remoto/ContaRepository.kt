package br.com.meubolso.data.remoto

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import br.com.meubolso.data.local.sincronizacao.ArmazemSincronizacao
import br.com.meubolso.data.local.sincronizacao.SincronizacaoDao
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import br.com.meubolso.di.EscopoAplicacao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.tasks.await
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

data class Usuario(val uid: String, val nome: String, val email: String)

sealed interface EstadoSessao {
    data object Carregando : EstadoSessao
    data object Deslogado : EstadoSessao
    /** Logado, mas ainda não criou nem entrou em um orçamento. */
    data class SemOrcamento(val usuario: Usuario) : EstadoSessao
    data class Pronto(val usuario: Usuario, val orcamentoId: String) : EstadoSessao
}

data class Orcamento(
    val id: String,
    val nome: String,
    val codigoConvite: String,
    /** uid → nome de quem participa. */
    val membros: Map<String, String>,
)

class CodigoConviteInvalidoException : Exception()

/** Há alterações locais que ainda não subiram; sair agora as perderia. */
class AlteracoesNaoEnviadasException : Exception()

/**
 * Conta (Firebase Auth) e orçamento compartilhado. O orçamento atual fica guardado no
 * DataStore para o app abrir sem internet.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class ContaRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val dataStore: DataStore<Preferences>,
    private val sincronizador: SincronizadorDados,
    private val armazem: ArmazemSincronizacao,
    private val sincronizacaoDao: SincronizacaoDao,
    @EscopoAplicacao escopo: CoroutineScope,
) {
    private val usuarioAtual: Flow<FirebaseUser?> = callbackFlow {
        val ouvinte = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(ouvinte)
        awaitClose { auth.removeAuthStateListener(ouvinte) }
    }

    private val preferencias: Flow<Preferences> = dataStore.data.catch { erro ->
        if (erro is IOException) emit(emptyPreferences()) else throw erro
    }

    /** Compartilhado: a tela e a sincronização observam o mesmo estado, sem buscas repetidas. */
    val estado: StateFlow<EstadoSessao> = usuarioAtual
        .map { it?.paraUsuario() }
        .distinctUntilChanged()
        .flatMapLatest { usuario ->
            if (usuario == null) return@flatMapLatest flowOf<EstadoSessao>(EstadoSessao.Deslogado)
            preferencias
                .map { prefs -> prefs[ORCAMENTO_ID]?.takeIf { prefs[ORCAMENTO_UID] == usuario.uid } }
                .distinctUntilChanged()
                .transformLatest { orcamentoId ->
                    if (orcamentoId != null) {
                        emit(EstadoSessao.Pronto(usuario, orcamentoId))
                        return@transformLatest
                    }
                    // Login novo neste aparelho: o orçamento pode já existir na nuvem.
                    emit(EstadoSessao.Carregando)
                    val remoto = runCatching { buscarOrcamentoDoUsuario(usuario.uid) }.getOrNull()
                    if (remoto != null) guardarOrcamento(usuario.uid, remoto) else emit(EstadoSessao.SemOrcamento(usuario))
                }
        }
        .stateIn(escopo, SharingStarted.Eagerly, EstadoSessao.Carregando)

    val orcamentoAtualId: Flow<String?> = estado.map { (it as? EstadoSessao.Pronto)?.orcamentoId }.distinctUntilChanged()

    fun observarOrcamento(id: String): Flow<Orcamento?> = callbackFlow {
        val registro = Firestore.orcamento(firestore, id).addSnapshotListener { snap, _ ->
            if (snap == null || !snap.exists()) return@addSnapshotListener
            @Suppress("UNCHECKED_CAST")
            trySend(
                Orcamento(
                    id = id,
                    nome = snap.getString(Firestore.CAMPO_NOME).orEmpty(),
                    codigoConvite = snap.getString(Firestore.CAMPO_CODIGO).orEmpty(),
                    membros = (snap.get(Firestore.CAMPO_NOMES_MEMBROS) as? Map<String, String>).orEmpty(),
                ),
            )
        }
        awaitClose { registro.remove() }
    }

    suspend fun entrarComEmail(email: String, senha: String) {
        auth.signInWithEmailAndPassword(email.trim(), senha).await()
    }

    suspend fun criarConta(nome: String, email: String, senha: String) {
        val usuario = auth.createUserWithEmailAndPassword(email.trim(), senha).await().user ?: return
        usuario.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(nome.trim()).build()).await()
        // Recarrega para o nome já valer ao criar ou entrar no orçamento.
        usuario.reload().await()
    }

    suspend fun entrarComGoogle(idToken: String) {
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
    }

    suspend fun redefinirSenha(email: String) {
        auth.sendPasswordResetEmail(email.trim()).await()
    }

    /** Cria o orçamento com um código de convite novo; os dados do aparelho sobem na sincronização. */
    suspend fun criarOrcamento(nome: String) {
        val usuario = auth.currentUser?.paraUsuario() ?: return
        val referencia = firestore.collection(Firestore.ORCAMENTOS).document()
        val codigo = gerarCodigoLivre()
        firestore.batch().apply {
            set(
                referencia,
                mapOf(
                    Firestore.CAMPO_NOME to nome.trim(),
                    Firestore.CAMPO_DONO to usuario.uid,
                    Firestore.CAMPO_MEMBROS to listOf(usuario.uid),
                    Firestore.CAMPO_NOMES_MEMBROS to mapOf(usuario.uid to usuario.nome),
                    Firestore.CAMPO_CODIGO to codigo,
                    Firestore.CAMPO_ALTERADO_EM to FieldValue.serverTimestamp(),
                ),
            )
            set(firestore.collection(Firestore.CONVITES).document(codigo), mapOf(Firestore.CAMPO_ORCAMENTO_ID to referencia.id))
            set(perfil(usuario.uid), dadosPerfil(usuario, referencia.id), SetOptions.merge())
        }.commit().await()
        guardarOrcamento(usuario.uid, referencia.id)
    }

    /** Entra no orçamento de outra pessoa; os dados deste aparelho se juntam aos dela. */
    suspend fun entrarComCodigo(codigo: String) {
        val usuario = auth.currentUser?.paraUsuario() ?: return
        val convite = firestore.collection(Firestore.CONVITES).document(codigo.trim().uppercase()).get().await()
        val orcamentoId = convite.getString(Firestore.CAMPO_ORCAMENTO_ID) ?: throw CodigoConviteInvalidoException()
        Firestore.orcamento(firestore, orcamentoId).update(
            mapOf(
                Firestore.CAMPO_MEMBROS to FieldValue.arrayUnion(usuario.uid),
                "${Firestore.CAMPO_NOMES_MEMBROS}.${usuario.uid}" to usuario.nome,
            ),
        ).await()
        perfil(usuario.uid).set(dadosPerfil(usuario, orcamentoId), SetOptions.merge()).await()
        guardarOrcamento(usuario.uid, orcamentoId)
    }

    /**
     * Envia o que falta e apaga os dados locais (eles ficam na nuvem). Se não conseguir enviar,
     * lança [AlteracoesNaoEnviadasException], a menos que [forcar].
     */
    suspend fun sair(forcar: Boolean = false) {
        val orcamentoId = orcamentoAtualId.first()
        if (orcamentoId == null) {
            // Ainda sem orçamento: os dados do aparelho não subiram para lugar nenhum, então ficam.
            auth.signOut()
            return
        }
        if (!forcar) {
            runCatching { sincronizador.sincronizar(orcamentoId) }
            if (sincronizacaoDao.observarQuantidadePendentes().first() > 0) throw AlteracoesNaoEnviadasException()
        }
        armazem.limparTudo()
        dataStore.edit {
            it.remove(ORCAMENTO_ID)
            it.remove(ORCAMENTO_UID)
        }
        auth.signOut()
    }

    private suspend fun buscarOrcamentoDoUsuario(uid: String): String? =
        perfil(uid).get().await().getString(Firestore.CAMPO_ORCAMENTO_ID)

    private suspend fun guardarOrcamento(uid: String, orcamentoId: String) {
        dataStore.edit {
            it[ORCAMENTO_ID] = orcamentoId
            it[ORCAMENTO_UID] = uid
        }
    }

    /** Código curto sem caracteres ambíguos (0/O, 1/I), que ainda não esteja em uso. */
    private suspend fun gerarCodigoLivre(): String {
        while (true) {
            val codigo = (1..TAMANHO_CODIGO).map { ALFABETO_CODIGO.random() }.joinToString("")
            if (!firestore.collection(Firestore.CONVITES).document(codigo).get().await().exists()) return codigo
        }
    }

    private fun perfil(uid: String) = firestore.collection(Firestore.USUARIOS).document(uid)

    private fun dadosPerfil(usuario: Usuario, orcamentoId: String) = mapOf(
        Firestore.CAMPO_NOME to usuario.nome,
        Firestore.CAMPO_EMAIL to usuario.email,
        Firestore.CAMPO_ORCAMENTO_ID to orcamentoId,
    )

    private fun FirebaseUser.paraUsuario() = Usuario(
        uid = uid,
        nome = displayName?.takeIf { it.isNotBlank() } ?: email?.substringBefore('@').orEmpty(),
        email = email.orEmpty(),
    )

    private companion object {
        val ORCAMENTO_ID = stringPreferencesKey("orcamento_id")
        val ORCAMENTO_UID = stringPreferencesKey("orcamento_uid")
        const val TAMANHO_CODIGO = 6
        const val ALFABETO_CODIGO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    }
}
