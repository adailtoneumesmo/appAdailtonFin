package br.com.meubolso

import android.app.Application
import br.com.meubolso.data.remoto.GerenciadorSincronizacao
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MeuBolsoApplication : Application() {

    @Inject
    lateinit var gerenciadorSincronizacao: GerenciadorSincronizacao

    override fun onCreate() {
        super.onCreate()
        gerenciadorSincronizacao.iniciar()
    }
}
