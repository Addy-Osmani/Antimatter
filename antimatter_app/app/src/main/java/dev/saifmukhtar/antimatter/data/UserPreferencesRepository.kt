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
        val CF_CLIENT_ID = stringPreferencesKey("cf_client_id")
        val CF_CLIENT_SECRET = stringPreferencesKey("cf_client_secret")
    }

    val savedCredentialsFlow: Flow<Triple<String?, String?, String?>> = context.dataStore.data
        .map { preferences ->
            Triple(
                preferences[WS_URL],
                preferences[CF_CLIENT_ID],
                preferences[CF_CLIENT_SECRET]
            )
        }

    suspend fun saveCredentials(url: String, clientId: String, clientSecret: String) {
        context.dataStore.edit { preferences ->
            preferences[WS_URL] = url
            preferences[CF_CLIENT_ID] = clientId
            preferences[CF_CLIENT_SECRET] = clientSecret
        }
    }

    suspend fun clearCredentials() {
        context.dataStore.edit { preferences ->
            preferences.remove(WS_URL)
            preferences.remove(CF_CLIENT_ID)
            preferences.remove(CF_CLIENT_SECRET)
        }
    }
}
