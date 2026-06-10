package dev.saifmukhtar.antimatter.feature.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.saifmukhtar.antimatter.core.network.InboundMessage
import dev.saifmukhtar.antimatter.core.network.OutboundMessage
import dev.saifmukhtar.antimatter.core.network.BridgeWebSocket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TerminalLine(
    val text: String,
    val isError: Boolean = false
)

data class TerminalUiState(
    val isUnlocked: Boolean = false,
    val outputLines: List<TerminalLine> = emptyList(),
    val isConnected: Boolean = false
)

@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val webSocket: BridgeWebSocket
) : ViewModel() {

    private val _uiState = MutableStateFlow(TerminalUiState())
    val uiState: StateFlow<TerminalUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            webSocket.connectionState.collect { state ->
                _uiState.update { it.copy(isConnected = state == BridgeWebSocket.ConnectionState.CONNECTED) }
            }
        }

        viewModelScope.launch {
            webSocket.messages.collect { message ->
                if (message is InboundMessage.CommandOutput) {
                    _uiState.update { state ->
                        val newLines = message.text.split("\n").map { line ->
                            TerminalLine(text = line, isError = message.isError)
                        }
                        // To avoid infinite memory, keep only last 1000 lines
                        val combined = state.outputLines + newLines
                        state.copy(outputLines = combined.takeLast(1000))
                    }
                }
            }
        }
    }

    fun unlockTerminal() {
        _uiState.update { it.copy(isUnlocked = true) }
    }

    fun executeCommand(command: String) {
        if (command.isBlank()) return

        // Echo the command locally
        _uiState.update { state ->
            val combined = state.outputLines + TerminalLine("> $command", isError = false)
            state.copy(outputLines = combined.takeLast(1000))
        }

        viewModelScope.launch {
            webSocket.sendMessage(OutboundMessage.ExecuteCommand(command = command))
        }
    }
    
    fun clearTerminal() {
        _uiState.update { it.copy(outputLines = emptyList()) }
    }
}
