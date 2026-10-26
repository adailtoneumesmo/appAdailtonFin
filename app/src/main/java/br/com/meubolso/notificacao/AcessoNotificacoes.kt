package br.com.meubolso.notificacao

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

/** A permissão de ler notificações só pode ser concedida pelo usuário, nas configurações do sistema. */
object AcessoNotificacoes {

    fun concedido(context: Context): Boolean =
        context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)

    /** Lista de apps com "Acesso a notificações". */
    fun abrirConfiguracao(context: Context) {
        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    /**
     * Informações do app: em APK instalado fora da loja, o Android 13+ bloqueia o acesso a
     * notificações até o usuário tocar em ⋮ → "Permitir configurações restritas" nesta tela.
     */
    fun abrirInformacoesApp(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
        )
    }
}
