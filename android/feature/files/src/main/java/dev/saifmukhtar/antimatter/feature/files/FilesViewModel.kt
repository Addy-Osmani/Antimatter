package dev.saifmukhtar.antimatter.feature.files

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import dev.saifmukhtar.antimatter.core.network.BridgeWebSocket
import dev.saifmukhtar.antimatter.core.network.FileNode
import dev.saifmukhtar.antimatter.core.network.InboundMessage
import dev.saifmukhtar.antimatter.core.network.OutboundMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

data class FilesUiState(
    val fileTree: List<FileNode>? = null,
    val isLoadingTree: Boolean = false,
    val viewedFilePath: String? = null,
    val viewedFileContent: String? = null,
    val viewedFileLanguage: String? = null,
    val isLoadingFile: Boolean = false,
    val allowedWorkspaces: List<String> = emptyList(),
    val currentWorkspace: String? = null
)

@HiltViewModel
class FilesViewModel @Inject constructor(
    private val webSocket: BridgeWebSocket
) : ViewModel() {

    private val _uiState = MutableStateFlow(FilesUiState())
    val uiState: StateFlow<FilesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.Unconfined) {
            webSocket.messages.collect { message ->
                when (message) {
                    is InboundMessage.FileTree -> {
                        android.util.Log.d("FilesViewModel", "Received FileTree with ${message.tree.size} items")
                        _uiState.update { it.copy(fileTree = message.tree, isLoadingTree = false) }
                    }
                    is InboundMessage.FileContent -> {
                        _uiState.update { 
                            it.copy(
                                viewedFileContent = message.content, 
                                viewedFileLanguage = message.language,
                                isLoadingFile = false
                            )
                        }
                    }
                    is InboundMessage.Error -> {
                        _uiState.update { it.copy(isLoadingTree = false, isLoadingFile = false) }
                    }
                    is InboundMessage.AvailableAgents -> {
                        _uiState.update { it.copy(allowedWorkspaces = message.allowedWorkspaces) }
                        // Also grab the current workspace from the active agent if any
                        message.agents.firstOrNull()?.workspaceRoot?.let { root ->
                            _uiState.update { it.copy(currentWorkspace = root) }
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    fun loadFileTree(path: String? = null) {
        android.util.Log.d("FilesViewModel", "loadFileTree called for path: $path")
        _uiState.update { it.copy(isLoadingTree = true) }
        webSocket.sendMessage(OutboundMessage.GetFiles(path))
    }

    fun openFile(path: String) {
        _uiState.update { it.copy(viewedFilePath = path, isLoadingFile = true, viewedFileContent = null) }
        webSocket.sendMessage(OutboundMessage.ReadFile(path))
    }

    fun closeFile() {
        _uiState.update { it.copy(viewedFilePath = null, viewedFileContent = null, viewedFileLanguage = null) }
    }

    fun writeFile(path: String, content: String) {
        webSocket.sendMessage(OutboundMessage.WriteFile(path, content))
        // Optimistically update viewed content
        _uiState.update { it.copy(viewedFileContent = content) }
    }

    fun changeWorkspace(path: String) {
        webSocket.sendMessage(OutboundMessage.ChangeWorkspace(path))
        _uiState.update { it.copy(currentWorkspace = path, isLoadingTree = true) }
    }
}
