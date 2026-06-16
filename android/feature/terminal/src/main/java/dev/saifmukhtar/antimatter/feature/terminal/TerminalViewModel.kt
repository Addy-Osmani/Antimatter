package dev.saifmukhtar.antimatter.feature.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import dev.saifmukhtar.antimatter.core.network.BridgeWebSocket
import dev.saifmukhtar.antimatter.core.network.InboundMessage
import dev.saifmukhtar.antimatter.core.network.OutboundMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import java.util.UUID

@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val webSocket: BridgeWebSocket
) : ViewModel(), TerminalSessionClient {

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _redrawEvent = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val redrawEvent = _redrawEvent.asSharedFlow()

    val terminalSession: TerminalSession = TerminalSession(this)
    private var ptyId: String? = null

    // Channel for buffering resize events so we can debounce them
    private val resizeChannel = Channel<Pair<Int, Int>>(Channel.CONFLATED)

    private var ioJob: Job? = null

    // Buffer for early PTY output before TerminalView creates the emulator
    private val pendingOutput = mutableListOf<ByteArray>()

    init {
        // Observe WebSocket events
        viewModelScope.launch(Dispatchers.Unconfined) {
            webSocket.connectionState.collect { state ->
                val connected = state == BridgeWebSocket.ConnectionState.CONNECTED
                _isConnected.value = connected
                if (connected) {
                    startPtySession()
                } else {
                    ptyId = null
                }
            }
        }

        viewModelScope.launch(Dispatchers.Unconfined) {
            webSocket.messages.collect { message ->
                if (message is InboundMessage.PtyOutput) {
                    handleMessage(message)
                }
            }
        }

        // Process debounced resize events
        viewModelScope.launch {
            resizeChannel.receiveAsFlow()
                .debounce(300L) // Wait 300ms before sending resize
                .collectLatest { (cols, rows) ->
                    val id = ptyId ?: return@collectLatest
                    val resizeMsg = OutboundMessage.PtyResize(id, cols, rows)
                    webSocket.sendMessage(resizeMsg)
                }
        }
    }

    private fun startPtySession() {
        val id = UUID.randomUUID().toString()
        ptyId = id
        val startMsg = OutboundMessage.PtyStart(id, 80, 24)
        webSocket.sendMessage(startMsg)
        startIoLoop()
    }

    private fun startIoLoop() {
        ioJob?.cancel()
        ioJob = viewModelScope.launch(Dispatchers.IO) {
            val buffer = ByteArray(4096)
            while (true) {
                // Read from TerminalSession (blocking read)
                val bytesRead = terminalSession.readInput(buffer, true)
                if (bytesRead > 0) {
                    // Send input over websocket
                    val id = ptyId
                    if (id != null) {
                        val dataB64 = android.util.Base64.encodeToString(buffer, 0, bytesRead, android.util.Base64.NO_WRAP)
                        val inputMsg = OutboundMessage.PtyInput(id, dataB64)
                        webSocket.sendMessage(inputMsg)
                    }
                } else {
                    delay(50) // Small delay if non-blocking returned 0, though readInput(..., true) is blocking
                }
            }
        }
    }

    private suspend fun handleMessage(message: InboundMessage.PtyOutput) {
        // The gateway base64-encodes all PTY output. We must decode before feeding the emulator.
        val bytes = android.util.Base64.decode(message.data, android.util.Base64.DEFAULT)
        withContext(Dispatchers.Main) {
            if (terminalSession.emulator != null) {
                terminalSession.appendToEmulator(bytes, bytes.size)
            } else {
                pendingOutput.add(bytes)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        ioJob?.cancel()
    }

    // Called by TerminalView when its size changes or emulator is first set
    fun onResize(cols: Int, rows: Int) {
        resizeChannel.trySend(Pair(cols, rows))
        
        // Flush any buffered output if emulator is now ready
        if (pendingOutput.isNotEmpty() && terminalSession.emulator != null) {
            val allBytes = pendingOutput.reduce { acc, bytes -> acc + bytes }
            terminalSession.appendToEmulator(allBytes, allBytes.size)
            pendingOutput.clear()
        }
    }

    // --- TerminalSessionClient implementation ---
    
    override fun onTextChanged(session: TerminalSession) {
        _redrawEvent.tryEmit(Unit)
    }

    override fun onTitleChanged(session: TerminalSession) {}

    override fun onSessionFinished(session: TerminalSession) {}

    override fun onCopyTextToClipboard(session: TerminalSession, text: String) {}

    override fun onPasteTextFromClipboard(session: TerminalSession?) {}

    override fun onBell(session: TerminalSession) {}

    override fun onColorsChanged(session: TerminalSession) {}

    override fun onTerminalCursorStateChange(state: Boolean) {}
    
    override fun setTerminalShellPid(session: TerminalSession, pid: Int) {}

    override fun getTerminalCursorStyle(): Int? = null

    override fun logError(tag: String?, message: String?) {}
    override fun logWarn(tag: String?, message: String?) {}
    override fun logInfo(tag: String?, message: String?) {}
    override fun logDebug(tag: String?, message: String?) {}
    override fun logVerbose(tag: String?, message: String?) {}
    override fun logStackTraceWithMessage(tag: String?, message: String?, e: Exception?) {}
    override fun logStackTrace(tag: String?, e: Exception?) {}
}
