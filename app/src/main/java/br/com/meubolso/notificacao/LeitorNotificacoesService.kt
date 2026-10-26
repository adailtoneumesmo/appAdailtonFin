package br.com.meubolso.notificacao

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import br.com.meubolso.data.repository.NotificacaoBancoRepository
import br.com.meubolso.domain.model.NotificacaoBanco
import br.com.meubolso.domain.notificacao.LeitorNotificacaoBanco
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * Recebe as notificações do aparelho (com a permissão "Acesso a notificações") e guarda as
 * dos bancos monitorados que mencionam valor, como pendências para o usuário confirmar.
 * Nada é lançado automaticamente e nada sai do aparelho.
 */
@AndroidEntryPoint
class LeitorNotificacoesService : NotificationListenerService() {

    @Inject
    lateinit var repository: NotificacaoBancoRepository

    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName || sbn.isOngoing) return
        val notificacao = sbn.notification
        // O resumo de um grupo repete o conteúdo das notificações filhas.
        if (notificacao.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        val banco = LeitorNotificacaoBanco.identificarBanco(sbn.packageName) ?: return

        val extras = notificacao.extras
        val titulo = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        // O texto expandido (BigTextStyle) costuma trazer a mensagem completa.
        val texto = (
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_TEXT)
            )?.toString().orEmpty()
        val completo = "$titulo\n$texto"
        if (!LeitorNotificacaoBanco.mencionaValor(completo)) return

        val tipo = LeitorNotificacaoBanco.sugerirTipo(completo)
        val registro = NotificacaoBanco(
            pacote = sbn.packageName,
            banco = banco.nome,
            titulo = titulo,
            texto = texto,
            recebidaEm = Instant.ofEpochMilli(sbn.postTime),
            valorCentavos = LeitorNotificacaoBanco.extrairValor(completo),
            descricaoSugerida = LeitorNotificacaoBanco.sugerirDescricao(texto, tipo)
                ?: LeitorNotificacaoBanco.sugerirDescricao(titulo, tipo),
            tipo = tipo,
            vale = banco.vale,
        )
        escopo.launch { repository.registrar(registro, chave = "${sbn.key}|$titulo|$texto") }
    }

    override fun onDestroy() {
        escopo.cancel()
        super.onDestroy()
    }
}
