package br.com.meubolso.ui.conta

import android.util.Log
import androidx.annotation.StringRes
import br.com.meubolso.R
import br.com.meubolso.data.remoto.CodigoConviteInvalidoException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException

@StringRes
fun mensagemErroConta(erro: Throwable): Int {
    Log.w("Conta", "Falha na conta", erro)
    return when (erro) {
        is FirebaseNetworkException -> R.string.conta_erro_sem_internet
        is FirebaseAuthWeakPasswordException -> R.string.conta_erro_senha_fraca
        is FirebaseAuthUserCollisionException -> R.string.conta_erro_email_em_uso
        is FirebaseAuthInvalidUserException, is FirebaseAuthInvalidCredentialsException -> R.string.conta_erro_credenciais
        is CodigoConviteInvalidoException -> R.string.conta_erro_codigo_invalido
        is FirebaseFirestoreException -> when (erro.code) {
            FirebaseFirestoreException.Code.UNAVAILABLE -> R.string.conta_erro_sem_internet
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> R.string.conta_erro_codigo_invalido
            else -> R.string.conta_erro_generico
        }
        else -> R.string.conta_erro_generico
    }
}
