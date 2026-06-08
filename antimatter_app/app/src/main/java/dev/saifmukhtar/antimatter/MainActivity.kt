package dev.saifmukhtar.antimatter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import dev.saifmukhtar.antimatter.network.BridgeWebSocket
import androidx.compose.ui.unit.dp
import dev.saifmukhtar.antimatter.ui.screens.ChatScreen
import dev.saifmukhtar.antimatter.ui.screens.ConnectScreen
import dev.saifmukhtar.antimatter.ui.screens.QRScannerScreen
import dev.saifmukhtar.antimatter.ui.screens.FilesScreen
import dev.saifmukhtar.antimatter.ui.screens.FileViewScreen
import dev.saifmukhtar.antimatter.ui.screens.DebugScreen
import androidx.compose.material.icons.filled.BugReport
import dev.saifmukhtar.antimatter.ui.theme.AntimatterTheme
import dev.saifmukhtar.antimatter.viewmodel.ChatViewModel
import dev.saifmukhtar.antimatter.viewmodel.FilesViewModel
import dev.saifmukhtar.antimatter.data.UserPreferencesRepository

class MainActivity : ComponentActivity() {

    private val chatViewModel: ChatViewModel by viewModels()
    private val filesViewModel: FilesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            AntimatterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val chatUiState by chatViewModel.uiState.collectAsState()
                    val filesUiState by filesViewModel.uiState.collectAsState()
                    val savedCredentials by chatViewModel.userPrefs.savedCredentialsFlow.collectAsState(initial = Triple(null, null, null))
                    val savedUrl = savedCredentials.first
                    
                    var currentTab by remember { mutableStateOf(0) }
                    var isScanningQR by remember { mutableStateOf(false) }

                    LaunchedEffect(intent) {
                        intent?.data?.let { uri ->
                            if (uri.scheme == "antimatter" && uri.host == "connect") {
                                val url = uri.getQueryParameter("url")
                                val token = uri.getQueryParameter("token")
                                if (url != null && token != null) {
                                    chatViewModel.connectManually(url, "token", token)
                                }
                            }
                        }
                    }
                    
                    if (isScanningQR) {
                        QRScannerScreen(
                            onQRScanned = { url, token, cfId, cfSecret ->
                                isScanningQR = false
                                // Since we decided the token isn't a Cloudflare client ID, but rather an auth token,
                                // we'll connect with it.
                                // Our QR URI: antimatter://connect?url=wss://...&token=...&cfid=...&cfsec=...
                                val fullUrl = "$url?token=$token"
                                chatViewModel.connectManually(fullUrl, cfId, cfSecret)
                            },
                            onNavigateBack = { isScanningQR = false }
                        )
                    } else if (!savedUrl.isNullOrEmpty()) {
                        if (filesUiState.viewedFilePath != null) {
                            FileViewScreen(
                                uiState = filesUiState,
                                onBack = { filesViewModel.closeFile() },
                                onSave = { path, content -> filesViewModel.writeFile(path, content) }
                            )
                        } else {
                            Scaffold(
                                bottomBar = {
                                    NavigationBar(
                                        modifier = Modifier.height(64.dp)
                                    ) {
                                        NavigationBarItem(
                                            icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chat") },
                                            selected = currentTab == 0,
                                            onClick = { currentTab = 0 },
                                            alwaysShowLabel = false
                                        )
                                        NavigationBarItem(
                                            icon = { Icon(Icons.Default.Folder, contentDescription = "Workspace") },
                                            selected = currentTab == 1,
                                            onClick = { currentTab = 1 },
                                            alwaysShowLabel = false
                                        )
                                        NavigationBarItem(
                                            icon = { Icon(Icons.Default.BugReport, contentDescription = "Debug") },
                                            selected = currentTab == 2,
                                            onClick = { currentTab = 2 },
                                            alwaysShowLabel = false
                                        )
                                    }
                                }
                            ) { paddingValues ->
                                Box(modifier = Modifier.padding(paddingValues)) {
                                    if (currentTab == 0) {
                                        ChatScreen(
                                            uiState = chatUiState,
                                            onSendPrompt = { chatViewModel.sendMessage(it) },
                                            onCancel = { chatViewModel.cancelResponse() },
                                            onNewConversation = { chatViewModel.startNewConversation() },
                                            onSubscribeConversation = { id -> chatViewModel.subscribeConversation(id) },
                                            onAcceptEdits = { chatViewModel.acceptEdits() },
                                            onRejectEdits = { chatViewModel.rejectEdits() },
                                            onChangeModel = { chatViewModel.changeModel() },
                                            onDisconnect = { chatViewModel.disconnectManually() }
                                        )
                                    } else if (currentTab == 1) {
                                        FilesScreen(
                                            uiState = filesUiState,
                                            onRefresh = { filesViewModel.loadFileTree() },
                                            onOpenFile = { path -> filesViewModel.openFile(path) },
                                            onCreateNode = { path, isDir -> filesViewModel.createNode(path, isDir) }
                                        )
                                    } else if (currentTab == 2) {
                                        DebugScreen(
                                            uiState = chatUiState,
                                            onContinue = { chatViewModel.debugContinue() },
                                            onStepOver = { chatViewModel.debugStepOver() },
                                            onStepInto = { chatViewModel.debugStepInto() },
                                            onStepOut = { chatViewModel.debugStepOut() },
                                            onRestart = { chatViewModel.debugRestart() },
                                            onStop = { chatViewModel.debugStop() }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        ConnectScreen(
                            connectionState = chatUiState.connectionState,
                            savedUrl = savedUrl,
                            savedClientId = savedCredentials.second,
                            savedClientSecret = savedCredentials.third,
                            onConnectClick = { url, clientId, clientSecret -> 
                                chatViewModel.connectManually(url, clientId, clientSecret) 
                            },
                            onScanQRClick = { isScanningQR = true }
                        )
                    }
                }
            }
        }
    }
}
