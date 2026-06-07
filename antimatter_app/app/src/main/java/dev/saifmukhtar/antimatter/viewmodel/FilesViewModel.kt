package dev.saifmukhtar.antimatter.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.saifmukhtar.antimatter.AntimatterApp
import dev.saifmukhtar.antimatter.network.BridgeWebSocket
import dev.saifmukhtar.antimatter.network.FileNode
import dev.saifmukhtar.antimatter.network.InboundMessage
import dev.saifmukhtar.antimatter.network.OutboundMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FilesUiState(
    val fileTree: List<FileNode>? = null,
    val isLoadingTree: Boolean = false,
    val viewedFilePath: String? = null,
    val viewedFileContent: String? = null,
    val viewedFileLanguage: String? = null,
    val isLoadingFile: Boolean = false
)

class FilesViewModel(application: Application) : AndroidViewModel(application) {

    private val webSocket: BridgeWebSocket
        get() = (getApplication<Application>() as AntimatterApp).webSocket

    private val _uiState = MutableStateFlow(FilesUiState())
    val uiState: StateFlow<FilesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            webSocket.messages.collect { message ->
                when (message) {
                    is InboundMessage.FileTree -> {
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
                    else -> {}
                }
            }
        }
    }

    fun loadFileTree(path: String? = null) {
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
}
