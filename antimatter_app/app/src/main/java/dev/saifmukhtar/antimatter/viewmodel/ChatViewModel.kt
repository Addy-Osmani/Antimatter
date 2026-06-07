package dev.saifmukhtar.antimatter.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.saifmukhtar.antimatter.AntimatterApp
import dev.saifmukhtar.antimatter.network.BridgeWebSocket
import dev.saifmukhtar.antimatter.network.InboundMessage
import dev.saifmukhtar.antimatter.network.OutboundMessage
import dev.saifmukhtar.antimatter.network.TrajectoryStep
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import dev.saifmukhtar.antimatter.data.UserPreferencesRepository

data class ChatUiState(
    val connectionState: BridgeWebSocket.ConnectionState = BridgeWebSocket.ConnectionState.DISCONNECTED,
    val isGenerating: Boolean = false,
    val steps: List<TrajectoryStep> = emptyList(),
    val conversationId: String? = null,
    val expectedStepCount: Int = 0,
    val activeFile: String? = null,
    val activeFileLanguage: String? = null,
    val cloudflareUrl: String? = null,
    val currentModel: String = "gemini-2.5-pro",
    val error: String? = null,
    val history: List<dev.saifmukhtar.antimatter.network.ConversationSummary> = emptyList()
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    
    private val webSocket: BridgeWebSocket
        get() = (getApplication<Application>() as AntimatterApp).webSocket

    private val userPrefs = UserPreferencesRepository(application)

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        // Observe connection state
        viewModelScope.launch {
            webSocket.connectionState.collect { state ->
                _uiState.update { it.copy(connectionState = state) }
            }
        }

        // Observe incoming messages
        viewModelScope.launch {
            webSocket.messages.collect { message ->
                handleInboundMessage(message)
            }
        }

        // Auto-connect on startup
        viewModelScope.launch {
            userPrefs.savedUrlFlow.collect { savedUrl ->
                if (!savedUrl.isNullOrBlank() && uiState.value.connectionState == BridgeWebSocket.ConnectionState.DISCONNECTED) {
                    webSocket.connect(savedUrl)
                }
            }
        }
    }

    private fun handleInboundMessage(message: InboundMessage) {
        when (message) {
            is InboundMessage.SessionState -> {
                _uiState.update {
                    it.copy(
                        conversationId = message.conversationId,
                        expectedStepCount = message.stepCount,
                        currentModel = message.model,
                        cloudflareUrl = message.cloudflareUrl
                    )
                }
                // If we connected and there's an active conversation, ask for it
                if (message.conversationId != null) {
                    webSocket.sendMessage(OutboundMessage.SubscribeConversation(message.conversationId))
                }
                webSocket.sendMessage(OutboundMessage.GetHistory())
            }
            is InboundMessage.Step -> {
                _uiState.update { state ->
                    // Replace if exists, otherwise append
                    val newSteps = state.steps.toMutableList()
                    if (message.index < newSteps.size) {
                        newSteps[message.index] = message.step
                    } else {
                        // Pad if we somehow missed a step (shouldn't happen with polling, but just in case)
                        while (newSteps.size < message.index) {
                            newSteps.add(TrajectoryStep(case = "unknown", value = "..."))
                        }
                        newSteps.add(message.step)
                    }
                    state.copy(steps = newSteps)
                }
            }
            is InboundMessage.Generating -> {
                _uiState.update {
                    it.copy(isGenerating = true, conversationId = message.conversationId)
                }
            }
            is InboundMessage.ResponseComplete -> {
                _uiState.update {
                    if (it.conversationId == message.conversationId) {
                        it.copy(isGenerating = false)
                    } else it
                }
            }
            is InboundMessage.ActiveFile -> {
                _uiState.update {
                    it.copy(activeFile = message.path, activeFileLanguage = message.language)
                }
            }
            is InboundMessage.CloudflareUrl -> {
                _uiState.update { it.copy(cloudflareUrl = message.url) }
            }
            is InboundMessage.HistoryList -> {
                _uiState.update { it.copy(history = message.conversations) }
            }
            is InboundMessage.Error -> {
                _uiState.update { it.copy(error = message.message) }
            }
            else -> {} // Handle FileTree, FileContent etc in a separate ViewModel or UI state
        }
    }

    // --- Actions ---

    fun connectManually(url: String) {
        viewModelScope.launch {
            userPrefs.saveUrl(url)
        }
        webSocket.connect(url)
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        
        // Optimistically add user step
        _uiState.update { state ->
            val newSteps = state.steps.toMutableList()
            newSteps.add(TrajectoryStep(case = "userInput", value = text))
            state.copy(steps = newSteps, isGenerating = true)
        }

        webSocket.sendMessage(OutboundMessage.SendMessage(text))
    }

    fun startNewConversation() {
        _uiState.update { it.copy(steps = emptyList(), conversationId = null, isGenerating = false) }
        webSocket.sendMessage(OutboundMessage.NewConversation())
        requestHistory() // Refresh history after starting new conversation
    }

    fun subscribeConversation(id: String) {
        _uiState.update { it.copy(steps = emptyList(), conversationId = id, isGenerating = false) }
        webSocket.sendMessage(OutboundMessage.SubscribeConversation(id))
    }

    fun requestHistory() {
        webSocket.sendMessage(OutboundMessage.GetHistory())
    }

    fun cancelResponse() {
        webSocket.sendMessage(OutboundMessage.CancelResponse())
        _uiState.update { it.copy(isGenerating = false) }
    }
    
    fun acceptEdits() {
        webSocket.sendMessage(OutboundMessage.AcceptEdits())
    }
    
    fun rejectEdits() {
        webSocket.sendMessage(OutboundMessage.RejectEdits())
    }
    
    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun disconnectManually() {
        viewModelScope.launch {
            userPrefs.clearUrl()
        }
        webSocket.disconnect()
    }

    override fun onCleared() {
        super.onCleared()
        webSocket.disconnect()
    }
}
