package dev.saifmukhtar.antimatter.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferencesRepository(private val context: Context) {

    companion object {
        val WS_URL = stringPreferencesKey("ws_url")
    }

    val savedUrlFlow: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[WS_URL]
        }

    suspend fun saveUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[WS_URL] = url
        }
    }

    suspend fun clearUrl() {
        context.dataStore.edit { preferences ->
            preferences.remove(WS_URL)
        }
    }
}
