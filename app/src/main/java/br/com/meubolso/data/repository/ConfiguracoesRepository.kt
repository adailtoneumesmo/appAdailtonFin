package br.com.meubolso.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfiguracoesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private val preferencias: Flow<Preferences> = dataStore.data.catch { erro ->
        if (erro is IOException) emit(emptyPreferences()) else throw erro
    }

    /** Antecipa recebimentos que caem em fim de semana/feriado para o dia útil anterior. Padrão: ligado. */
    val anteciparDiaNaoUtil: Flow<Boolean> =
        preferencias.map { it[ANTECIPAR_DIA_NAO_UTIL] ?: true }

    suspend fun definirAnteciparDiaNaoUtil(ativo: Boolean) {
        dataStore.edit { it[ANTECIPAR_DIA_NAO_UTIL] = ativo }
    }

    private companion object {
        val ANTECIPAR_DIA_NAO_UTIL = booleanPreferencesKey("antecipar_dia_nao_util")
    }
}
