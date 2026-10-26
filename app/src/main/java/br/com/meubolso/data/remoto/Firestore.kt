package br.com.meubolso.data.remoto

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Estrutura no Firestore (as regras de acesso estão em `firestore.rules`, na raiz do projeto):
 *
 * - `usuarios/{uid}`: orçamento do usuário
 * - `orcamentos/{id}`: nome, membros e código de convite
 * - `orcamentos/{id}/{tabela}/{uuid}`: os dados, uma coleção por tabela do Room
 * - `convites/{codigo}`: aponta para o orçamento, para quem entra com o código
 */
object Firestore {
    const val USUARIOS = "usuarios"
    const val ORCAMENTOS = "orcamentos"
    const val CONVITES = "convites"

    const val CAMPO_ORCAMENTO_ID = "orcamentoId"
    const val CAMPO_NOME = "nome"
    const val CAMPO_EMAIL = "email"
    const val CAMPO_DONO = "donoUid"
    const val CAMPO_MEMBROS = "membros"
    const val CAMPO_NOMES_MEMBROS = "nomesMembros"
    const val CAMPO_CODIGO = "codigoConvite"
    const val CAMPO_ALTERADO_EM = "alteradoEm"

    /** Campos de controle nos documentos de dados; não são colunas do Room. */
    const val PREFIXO_META = "meta_"
    const val CAMPO_ATUALIZADO_EM = "meta_atualizado_em"
    const val CAMPO_SINCRONIZADO_EM = "meta_sincronizado_em"
    const val CAMPO_EXCLUIDO = "meta_excluido"

    fun orcamento(firestore: FirebaseFirestore, id: String): DocumentReference =
        firestore.collection(ORCAMENTOS).document(id)

    fun colecao(firestore: FirebaseFirestore, orcamentoId: String, tabela: String): CollectionReference =
        orcamento(firestore, orcamentoId).collection(tabela)
}
