package dev.saifmukhtar.antimatter.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.saifmukhtar.antimatter.core.network.BridgeWebSocket
import dev.saifmukhtar.antimatter.core.network.InboundMessage
import dev.saifmukhtar.antimatter.core.network.OutboundMessage
import dev.saifmukhtar.antimatter.core.network.TrajectoryStep
import dev.saifmukhtar.antimatter.core.network.StepCase
import dev.saifmukhtar.antimatter.core.data.GzipUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import dev.saifmukhtar.antimatter.core.data.UserPreferencesRepository
import dev.saifmukhtar.antimatter.core.data.AppDatabase
import dev.saifmukhtar.antimatter.core.data.ConversationEntity
import dev.saifmukhtar.antimatter.core.data.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import dev.saifmukhtar.antimatter.core.data.AppDao

data class ChatUiState(
    val connectionState: BridgeWebSocket.ConnectionState = BridgeWebSocket.ConnectionState.DISCONNECTED,
    val isGenerating: Boolean = false,
    val steps: List<TrajectoryStep> = emptyList(),
    val conversationId: String? = null,
    val expectedStepCount: Int = 0,
    val activeFile: String? = null,
    val activeFileLanguage: String? = null,
    val cloudflareUrl: String? = null,
    val environment: String? = null,
    val currentModel: String = "gemini-2.5-pro",
    val error: String? = null,
    val history: List<dev.saifmukhtar.antimatter.core.network.ConversationSummary> = emptyList(),
    val terminalOutput: String = "",
    val artifacts: List<dev.saifmukhtar.antimatter.core.network.FileNode> = emptyList(),
    val activeArtifactContent: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val webSocket: BridgeWebSocket,
    val userPrefs: UserPreferencesRepository,
    private val appDao: AppDao
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val scrollStateFlow = MutableSharedFlow<Pair<Int, Int>>(extraBufferCapacity = 1, onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST)

    init {
        @OptIn(kotlinx.coroutines.FlowPreview::class)
        viewModelScope.launch {
            scrollStateFlow.debounce(500).collect { (index, offset) ->
                _uiState.value.conversationId?.let { cid ->
                    withContext(Dispatchers.IO) {
                        appDao.updateScrollState(cid, index, offset)
                    }
                }
            }
        }
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
    }

    private fun handleInboundMessage(message: InboundMessage) {
        when (message) {
            is InboundMessage.SessionState -> {
                val previousConversationId = _uiState.value.conversationId
                
                _uiState.update {
                    it.copy(
                        conversationId = message.conversationId,
                        expectedStepCount = message.stepCount,
                        currentModel = message.model,
                        cloudflareUrl = message.cloudflareUrl,
                        environment = message.environment
                    )
                }
                
                // Truncate local steps if server says there are fewer (due to edit)
                message.conversationId?.let { cid ->
                    viewModelScope.launch(Dispatchers.IO) {
                        appDao.deleteStepsFromIndex(cid, message.stepCount)
                        appDao.updateStepCount(cid, message.stepCount)
                    }
                }
                val cid = message.conversationId
                // Only subscribe if the conversation actually changed
                if (cid != null && cid != previousConversationId) {
                    subscribeConversation(cid)
                }
                webSocket.sendMessage(OutboundMessage.GetHistory())
            }
            is InboundMessage.Step -> {
                _uiState.update { state ->
                    val newSteps = state.steps.toMutableList()
                    if (message.index < newSteps.size) {
                        newSteps[message.index] = message.step
                    } else {
                        while (newSteps.size < message.index) {
                            newSteps.add(TrajectoryStep(case = "unknown", value = "..."))
                        }
                        newSteps.add(message.step)
                    }
                    state.conversationId?.let { cid ->
                        viewModelScope.launch(Dispatchers.IO) {
                            appDao.insertSteps(listOf(message.step.toEntity(cid, message.index)))
                        }
                    }
                    var stillGenerating = state.isGenerating
                    when (message.step.stepCase) {
                        StepCase.PLANNER_RESPONSE, StepCase.TOOL_CALL, StepCase.RUN_COMMAND -> stillGenerating = true
                        StepCase.TEXT, StepCase.MARKDOWN_CHUNK, StepCase.ERROR_MESSAGE, 
                        StepCase.APPROVAL_INTERACTION, StepCase.ASK_QUESTION, StepCase.ELICITATION -> stillGenerating = false
                        else -> {}
                    }
                    state.copy(steps = newSteps, isGenerating = stillGenerating)
                }
            }
            is InboundMessage.StepBatch -> {
                _uiState.update { state ->
                    val newSteps = state.steps.toMutableList()
                    var stillGenerating = state.isGenerating
                    message.steps.forEach { batchStep ->
                        if (batchStep.index < newSteps.size) {
                            newSteps[batchStep.index] = batchStep.step
                        } else {
                            while (newSteps.size < batchStep.index) {
                                newSteps.add(TrajectoryStep(case = "unknown", value = "..."))
                            }
                            newSteps.add(batchStep.step)
                        }
                        // Self-regulate generating state based on step types
                        when (batchStep.step.stepCase) {
                            StepCase.PLANNER_RESPONSE, StepCase.TOOL_CALL, StepCase.RUN_COMMAND -> stillGenerating = true
                            StepCase.TEXT, StepCase.MARKDOWN_CHUNK, StepCase.ERROR_MESSAGE, 
                            StepCase.APPROVAL_INTERACTION, StepCase.ASK_QUESTION, StepCase.ELICITATION -> stillGenerating = false
                            else -> {}
                        }
                    }
                    state.conversationId?.let { cid ->
                        viewModelScope.launch(Dispatchers.IO) {
                            appDao.insertSteps(message.steps.map { it.step.toEntity(cid, it.index) })
                        }
                    }
                    state.copy(steps = newSteps, isGenerating = stillGenerating)
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
                android.util.Log.d("ChatViewModel", "Received HISTORY_LIST with ${message.conversations.size} conversations")
                viewModelScope.launch(Dispatchers.IO) {
                    message.conversations.forEach { summary ->
                        val existing = appDao.getConversation(summary.id)
                        if (existing == null) {
                            appDao.insertConversation(ConversationEntity(id = summary.id, title = summary.title, timestamp = summary.timestamp))
                        }
                    }
                }
                _uiState.update { it.copy(history = message.conversations) }
            }
            is InboundMessage.TerminalOutput -> {
                _uiState.update { it.copy(terminalOutput = it.terminalOutput + message.content) }
            }
            is InboundMessage.ArtifactsList -> {
                _uiState.update { it.copy(artifacts = message.artifacts) }
                _uiState.value.conversationId?.let { cid ->
                    viewModelScope.launch(Dispatchers.IO) {
                        val entities = message.artifacts.map {
                            val existing = appDao.getArtifact(cid, it.path)
                            dev.saifmukhtar.antimatter.core.data.ArtifactEntity(
                                conversationId = cid,
                                path = it.path,
                                name = it.name,
                                compressedContent = existing?.compressedContent
                            )
                        }
                        appDao.insertArtifacts(entities)
                    }
                }
            }
            is InboundMessage.FileContent -> {
                // If it's a markdown artifact we requested, we assume it gets populated here.
                if (message.path.endsWith(".md")) {
                    _uiState.update { it.copy(activeArtifactContent = message.content) }
                    _uiState.value.conversationId?.let { cid ->
                        viewModelScope.launch(Dispatchers.IO) {
                            val compressed = GzipUtils.compress(message.content)
                            val name = message.path.substringAfterLast("/")
                            appDao.insertArtifacts(listOf(
                                dev.saifmukhtar.antimatter.core.data.ArtifactEntity(
                                    conversationId = cid,
                                    path = message.path,
                                    name = name,
                                    compressedContent = compressed
                                )
                            ))
                        }
                    }
                }
            }
            is InboundMessage.Error -> {
                _uiState.update { it.copy(error = message.message) }
                // Auto-dismiss error after 5 seconds
                viewModelScope.launch {
                    kotlinx.coroutines.delay(5000)
                    _uiState.update { if (it.error == message.message) it.copy(error = null) else it }
                }
            }
            else -> {} // Handle FileTree, FileContent etc in a separate ViewModel or UI state
        }
    }

    // --- Actions ---

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
        viewModelScope.launch {
            // Instantly load cached steps from Room
            val cachedSteps = withContext(Dispatchers.IO) {
                appDao.getStepsForConversation(id)
            }
            val initialSteps = cachedSteps.map { it.toTrajectoryStep() }
            
            _uiState.update { it.copy(steps = initialSteps, conversationId = id, isGenerating = false) }
            
            // Send delta sync request with lastKnownStepCount
            webSocket.sendMessage(OutboundMessage.SubscribeConversation(id, lastKnownStepCount = initialSteps.size))
        }
    }

    fun requestHistory() {
        webSocket.sendMessage(OutboundMessage.GetHistory())
    }

    fun requestArtifacts() {
        _uiState.value.conversationId?.let { cid ->
            viewModelScope.launch {
                val cached = withContext(Dispatchers.IO) {
                    appDao.getArtifactsForConversation(cid)
                }
                if (cached.isNotEmpty()) {
                    val converted = cached.map { 
                        dev.saifmukhtar.antimatter.core.network.FileNode(name = it.name, path = it.path, isDir = false)
                    }
                    _uiState.update { it.copy(artifacts = converted) }
                }
                webSocket.sendMessage(OutboundMessage.GetArtifacts(cid))
            }
        }
    }

    fun requestArtifactContent(path: String) {
        _uiState.value.conversationId?.let { cid ->
            viewModelScope.launch {
                val cached = withContext(Dispatchers.IO) {
                    appDao.getArtifact(cid, path)
                }
                val compressedContent = cached?.compressedContent
                if (compressedContent != null) {
                    try {
                        val decompressed = GzipUtils.decompress(compressedContent)
                        _uiState.update { it.copy(activeArtifactContent = decompressed) }
                    } catch (e: Exception) {
                        android.util.Log.e("ChatViewModel", "Failed to decompress artifact", e)
                    }
                }
                webSocket.sendMessage(OutboundMessage.ReadFile(path))
            }
        }
    }

    fun clearActiveArtifact() {
        _uiState.update { it.copy(activeArtifactContent = null) }
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
    
    fun changeModel() {
        webSocket.sendMessage(OutboundMessage.ChangeModel())
    }
    
    
    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun updateScrollState(index: Int, offset: Int) {
        scrollStateFlow.tryEmit(Pair(index, offset))
    }

    suspend fun getScrollState(id: String): Pair<Int, Int> {
        return withContext(Dispatchers.IO) {
            val conv = appDao.getConversation(id)
            Pair(conv?.scrollIndex ?: 0, conv?.scrollOffset ?: 0)
        }
    }

    override fun onCleared() {
        super.onCleared()
        webSocket.disconnect()
    }
}
