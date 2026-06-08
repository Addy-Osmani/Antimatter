package dev.saifmukhtar.antimatter.network

import android.content.Context
import android.content.Intent
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import kotlin.math.pow

class BridgeWebSocket(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // Disable timeout for websocket
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    
    private var currentUrl: String? = null
    private var clientId: String? = null
    private var clientSecret: String? = null
    private var reconnectAttempt = 0
    private var isConnecting = false
    private var manuallyDisconnected = false

    // State flows
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    private fun updateConnectionState(newState: ConnectionState) {
        _connectionState.update { newState }
        if (newState == ConnectionState.CONNECTED) {
            val intent = Intent(context, BridgeService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } else if (newState == ConnectionState.DISCONNECTED) {
            context.stopService(Intent(context, BridgeService::class.java))
        }
    }
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _messages = MutableSharedFlow<InboundMessage>(extraBufferCapacity = 64)
    val messages: SharedFlow<InboundMessage> = _messages

    enum class ConnectionState {
        DISCONNECTED, CONNECTING, CONNECTED
    }

    init {
        // No init needed
    }

    fun connect(url: String, clientId: String? = null, clientSecret: String? = null) {
        manuallyDisconnected = false
        currentUrl = url
        this.clientId = clientId?.takeIf { it.isNotBlank() }
        this.clientSecret = clientSecret?.takeIf { it.isNotBlank() }
        connectInternal()
    }

    fun disconnect() {
        manuallyDisconnected = true
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        updateConnectionState(ConnectionState.DISCONNECTED)
    }

    private fun connectInternal() {
        if (isConnecting || currentUrl == null || manuallyDisconnected) return
        isConnecting = true
        updateConnectionState(ConnectionState.CONNECTING)

        try {
            val requestBuilder = Request.Builder()
                .url(currentUrl!!)
                .header("Bypass-Tunnel-Reminder", "true")
                
            if (clientId != null && clientSecret != null) {
                requestBuilder.header("CF-Access-Client-Id", clientId!!)
                requestBuilder.header("CF-Access-Client-Secret", clientSecret!!)
            }
                
            val request = requestBuilder.build()

            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d("BridgeWebSocket", "Connected to $currentUrl")
                    isConnecting = false
                    reconnectAttempt = 0
                    updateConnectionState(ConnectionState.CONNECTED)
                }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("BridgeWebSocket", "Received: ${text.take(100)}")
                scope.launch {
                    try {
                        val jsonObject = JsonParser.parseString(text).asJsonObject
                        val type = jsonObject.get("type").asString
                        val message = when (type) {
                            "PONG" -> InboundMessage.Pong()
                            "SESSION_STATE" -> gson.fromJson(text, InboundMessage.SessionState::class.java)
                            "STEP" -> gson.fromJson(text, InboundMessage.Step::class.java)
                            "STEP_BATCH" -> gson.fromJson(text, InboundMessage.StepBatch::class.java)
                            "GENERATING" -> gson.fromJson(text, InboundMessage.Generating::class.java)
                            "RESPONSE_COMPLETE" -> gson.fromJson(text, InboundMessage.ResponseComplete::class.java)
                            "ACTIVE_FILE" -> gson.fromJson(text, InboundMessage.ActiveFile::class.java)
                            "FILE_CONTENT" -> gson.fromJson(text, InboundMessage.FileContent::class.java)
                            "FILE_TREE" -> gson.fromJson(text, InboundMessage.FileTree::class.java)
                            "CLOUDFLARE_URL" -> gson.fromJson(text, InboundMessage.CloudflareUrl::class.java)
                            "DEBUG_STATUS" -> gson.fromJson(text, InboundMessage.DebugStatus::class.java)
                            "TERMINAL_OUTPUT" -> gson.fromJson(text, InboundMessage.TerminalOutput::class.java)
                            "HISTORY_LIST" -> gson.fromJson(text, InboundMessage.HistoryList::class.java)
                            "ERROR" -> gson.fromJson(text, InboundMessage.Error::class.java)
                            "SYSTEM_ALERT" -> gson.fromJson(text, InboundMessage.SystemAlert::class.java)
                            else -> InboundMessage.Unknown
                        }
                        _messages.emit(message)
                    } catch (e: Exception) {
                        Log.e("BridgeWebSocket", "Failed to parse message: $text", e)
                    }
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("BridgeWebSocket", "Closed: $reason")
                isConnecting = false
                updateConnectionState(ConnectionState.DISCONNECTED)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("BridgeWebSocket", "Failure: ${t.message}")
                isConnecting = false
                updateConnectionState(ConnectionState.DISCONNECTED)
                scheduleReconnect()
            }
        })
        } catch (e: Exception) {
            isConnecting = false
            updateConnectionState(ConnectionState.DISCONNECTED)
            Log.e("BridgeWebSocket", "Failed to create request", e)
        }
    }

    private fun scheduleReconnect() {
        if (manuallyDisconnected || currentUrl == null) return
        if (reconnectAttempt >= 20) return // Max retries reached
        
        reconnectAttempt++
        // Exponential backoff: max 30s
        val delayMs = (2.0.pow(reconnectAttempt.coerceAtMost(5)).toLong() * 1000).coerceAtMost(30000)
        
        Log.d("BridgeWebSocket", "Scheduling reconnect in ${delayMs}ms (Attempt $reconnectAttempt)")
        scope.launch {
            delay(delayMs)
            connectInternal()
        }
    }

    fun sendMessage(message: OutboundMessage) {
        val json = gson.toJson(message)
        Log.d("BridgeWebSocket", "Sending: ${json.take(100)}")
        webSocket?.send(json)
    }
}
