package dev.saifmukhtar.antimatter.core.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.SecureRandom
import android.util.Base64

data class Credentials(val url: String?, val clientId: String?, val clientSecret: String?, val token: String?, val pubKey: String?)

class UserPreferencesRepository(private val context: Context) {

    companion object {
        const val WS_URL = "ws_url"
        const val CF_CLIENT_ID = "cf_client_id"
        const val CF_CLIENT_SECRET = "cf_client_secret"
        const val TOKEN = "auth_token"
        const val PUBKEY = "pub_key"
        const val DB_PASSPHRASE = "db_passphrase"
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

    private val _savedCredentialsFlow = MutableStateFlow<Credentials>(
        Credentials(
            securePrefs.getString(WS_URL, null),
            securePrefs.getString(CF_CLIENT_ID, null),
            securePrefs.getString(CF_CLIENT_SECRET, null),
            securePrefs.getString(TOKEN, null),
            securePrefs.getString(PUBKEY, null)
        )
    )
    
    val savedCredentialsFlow: StateFlow<Credentials> = _savedCredentialsFlow.asStateFlow()

    suspend fun saveCredentials(url: String, clientId: String, clientSecret: String, token: String?, pubKey: String?) {
        securePrefs.edit()
            .putString(WS_URL, url)
            .putString(CF_CLIENT_ID, clientId)
            .putString(CF_CLIENT_SECRET, clientSecret)
            .putString(TOKEN, token)
            .putString(PUBKEY, pubKey)
            .apply()
            
        _savedCredentialsFlow.value = Credentials(url, clientId, clientSecret, token, pubKey)
    }

    suspend fun clearCredentials() {
        // We DO NOT clear the DB passphrase when clearing credentials, otherwise the user loses access to their local DB!
        securePrefs.edit()
            .remove(WS_URL)
            .remove(CF_CLIENT_ID)
            .remove(CF_CLIENT_SECRET)
            .remove(TOKEN)
            .remove(PUBKEY)
            .apply()
        _savedCredentialsFlow.value = Credentials(null, null, null, null, null)
    }

    fun getDatabasePassphrase(): ByteArray {
        var passphrase = securePrefs.getString(DB_PASSPHRASE, null)
        if (passphrase == null) {
            val secureRandom = SecureRandom()
            val key = ByteArray(32) // 256 bits
            secureRandom.nextBytes(key)
            passphrase = Base64.encodeToString(key, Base64.NO_WRAP)
            securePrefs.edit().putString(DB_PASSPHRASE, passphrase).apply()
        }
        return passphrase.toByteArray()
    }
}
