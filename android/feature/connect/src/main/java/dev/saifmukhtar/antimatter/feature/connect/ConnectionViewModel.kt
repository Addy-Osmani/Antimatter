package dev.saifmukhtar.antimatter.feature.connect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.saifmukhtar.antimatter.core.data.UserPreferencesRepository
import dev.saifmukhtar.antimatter.core.network.BridgeWebSocket
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConnectionViewModel @Inject constructor(
    private val webSocket: BridgeWebSocket,
    private val userPrefs: UserPreferencesRepository
) : ViewModel() {

    val connectionState: StateFlow<BridgeWebSocket.ConnectionState> = webSocket.connectionState

    val savedCredentialsFlow = userPrefs.savedCredentialsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = dev.saifmukhtar.antimatter.core.data.Credentials(null, null, null, null, null)
    )

    val profilesFlow = userPrefs.profilesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            // Wait for UserPreferencesRepository to load
            userPrefs.isLoadedFlow.filter { it }.first()
            
            // One-time auto-connect on startup
            val creds = userPrefs.savedCredentialsFlow.value
            val url = creds.url
            if (!url.isNullOrEmpty() && webSocket.connectionState.value == BridgeWebSocket.ConnectionState.DISCONNECTED) {
                webSocket.connect(url, creds.clientId, creds.clientSecret, creds.token, creds.pubKey)
            }
        }
    }

    fun connectManually(url: String, cfId: String?, cfSecret: String?, token: String? = null, pubKey: String? = null) {
        viewModelScope.launch {
            userPrefs.saveCredentials(url, cfId ?: "", cfSecret ?: "", token, pubKey)
            webSocket.connect(url, cfId, cfSecret, token, pubKey)
        }
    }

    fun connectNamedProfile(name: String, url: String, cfId: String?, cfSecret: String?, token: String?, pubKey: String?) {
        viewModelScope.launch {
            val creds = dev.saifmukhtar.antimatter.core.data.Credentials(url, cfId ?: "", cfSecret ?: "", token, pubKey)
            userPrefs.saveProfile(name, creds)
            webSocket.connect(url, cfId, cfSecret, token, pubKey)
        }
    }

    fun disconnectManually() {
        webSocket.disconnect()
        viewModelScope.launch {
            userPrefs.clearCredentials()
        }
    }

    fun switchProfile(id: String) {
        viewModelScope.launch {
            userPrefs.setActiveProfile(id)
            val creds = userPrefs.savedCredentialsFlow.value
            val url = creds.url
            if (!url.isNullOrEmpty()) {
                webSocket.connect(url, creds.clientId, creds.clientSecret, creds.token, creds.pubKey)
            }
        }
    }

    fun deleteProfile(id: String) {
        viewModelScope.launch {
            userPrefs.deleteProfile(id)
        }
    }
}
