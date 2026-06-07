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
import dev.saifmukhtar.antimatter.ui.screens.FilesScreen
import dev.saifmukhtar.antimatter.ui.screens.FileViewScreen
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
                    val savedUrl by UserPreferencesRepository(this@MainActivity).savedUrlFlow.collectAsState(initial = null)
                    
                    var currentTab by remember { mutableStateOf(0) }
                    
                    if (!savedUrl.isNullOrEmpty()) {
                        if (filesUiState.viewedFilePath != null) {
                            FileViewScreen(
                                uiState = filesUiState,
                                onBack = { filesViewModel.closeFile() }
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
                                            onAcceptEdits = { /* TODO */ },
                                            onRejectEdits = { /* TODO */ },
                                            onDisconnect = { chatViewModel.disconnectManually() }
                                        )
                                    } else {
                                        FilesScreen(
                                            uiState = filesUiState,
                                            onRefresh = { filesViewModel.loadFileTree() },
                                            onOpenFile = { path -> filesViewModel.openFile(path) }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        ConnectScreen(
                            connectionState = chatUiState.connectionState,
                            savedUrl = savedUrl,
                            onConnectClick = { url -> chatViewModel.connectManually(url) }
                        )
                    }
                }
            }
        }
    }
}
