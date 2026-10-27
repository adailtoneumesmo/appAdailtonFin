package br.com.meubolso.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import br.com.meubolso.data.local.MeuBolsoDatabase
import br.com.meubolso.data.local.cartao.CartaoDao
import br.com.meubolso.data.local.categoria.CategoriaDao
import br.com.meubolso.data.local.despesa.DespesaDao
import br.com.meubolso.data.local.listacompra.ListaCompraDao
import br.com.meubolso.data.local.notificacao.NotificacaoBancoDao
import br.com.meubolso.data.local.objetivo.ObjetivoDao
import br.com.meubolso.data.local.renda.RendaDao
import br.com.meubolso.data.local.sincronizacao.SincronizacaoDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MeuBolsoDatabase =
        Room.databaseBuilder(context, MeuBolsoDatabase::class.java, MeuBolsoDatabase.NOME)
            .addMigrations(*MeuBolsoDatabase.MIGRACOES)
            .addCallback(MeuBolsoDatabase.AoCriar)
            .build()

    @Provides
    fun provideRendaDao(database: MeuBolsoDatabase): RendaDao = database.rendaDao()

    @Provides
    fun provideCategoriaDao(database: MeuBolsoDatabase): CategoriaDao = database.categoriaDao()

    @Provides
    fun provideDespesaDao(database: MeuBolsoDatabase): DespesaDao = database.despesaDao()

    @Provides
    fun provideCartaoDao(database: MeuBolsoDatabase): CartaoDao = database.cartaoDao()

    @Provides
    fun provideListaCompraDao(database: MeuBolsoDatabase): ListaCompraDao = database.listaCompraDao()

    @Provides
    fun provideNotificacaoBancoDao(database: MeuBolsoDatabase): NotificacaoBancoDao = database.notificacaoBancoDao()

    @Provides
    fun provideObjetivoDao(database: MeuBolsoDatabase): ObjetivoDao = database.objetivoDao()

    @Provides
    fun provideSincronizacaoDao(database: MeuBolsoDatabase): SincronizacaoDao = database.sincronizacaoDao()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("configuracoes") }

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()
}
