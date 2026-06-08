package dev.saifmukhtar.antimatter.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferencesRepository(private val context: Context) {

    companion object {
        const val WS_URL = "ws_url"
        const val CF_CLIENT_ID = "cf_client_id"
        const val CF_CLIENT_SECRET = "cf_client_secret"
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val securePrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _savedCredentialsFlow = MutableStateFlow<Triple<String?, String?, String?>>(
        Triple(
            securePrefs.getString(WS_URL, null),
            securePrefs.getString(CF_CLIENT_ID, null),
            securePrefs.getString(CF_CLIENT_SECRET, null)
        )
    )
    
    val savedCredentialsFlow: StateFlow<Triple<String?, String?, String?>> = _savedCredentialsFlow.asStateFlow()

    suspend fun saveCredentials(url: String, clientId: String, clientSecret: String) {
        securePrefs.edit()
            .putString(WS_URL, url)
            .putString(CF_CLIENT_ID, clientId)
            .putString(CF_CLIENT_SECRET, clientSecret)
            .apply()
            
        _savedCredentialsFlow.value = Triple(url, clientId, clientSecret)
    }

    suspend fun clearCredentials() {
        securePrefs.edit().clear().apply()
        _savedCredentialsFlow.value = Triple(null, null, null)
    }
}
