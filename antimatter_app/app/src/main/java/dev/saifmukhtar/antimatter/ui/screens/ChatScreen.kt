package dev.saifmukhtar.antimatter.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.saifmukhtar.antimatter.network.BridgeWebSocket
import dev.saifmukhtar.antimatter.network.StepCase
import dev.saifmukhtar.antimatter.viewmodel.ChatUiState
import dev.saifmukhtar.antimatter.ui.components.ChatBubble
import dev.saifmukhtar.antimatter.ui.components.MessageInput
import dev.saifmukhtar.antimatter.ui.components.ThinkingBubble
import dev.saifmukhtar.antimatter.ui.components.ToolCallCard
import dev.saifmukhtar.antimatter.ui.components.TypingIndicator
import io.noties.markwon.Markwon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    uiState: ChatUiState,
    onSendPrompt: (String) -> Unit,
    onCancel: () -> Unit,
    onNewConversation: () -> Unit,
    onSubscribeConversation: (String) -> Unit,
    onAcceptEdits: () -> Unit,
    onRejectEdits: () -> Unit,
    onDisconnect: () -> Unit
) {
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val markwon = remember { Markwon.create(context) }

    var isInitialLoad by remember { mutableStateOf(true) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(uiState.steps.size) {
        if (uiState.steps.isNotEmpty()) {
            if (isInitialLoad || uiState.steps.size <= uiState.expectedStepCount) {
                listState.scrollToItem(uiState.steps.size - 1)
                if (uiState.steps.size >= uiState.expectedStepCount) {
                    isInitialLoad = false
                }
            } else {
                listState.animateScrollToItem(uiState.steps.size - 1)
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                Text(
                    "Chat History",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge
                )
                HorizontalDivider()
                LazyColumn {
                    items(uiState.history) { conversation ->
                        NavigationDrawerItem(
                            label = { Text(conversation.title, maxLines = 1) },
                            selected = conversation.id == uiState.conversationId,
                            onClick = {
                                onSubscribeConversation(conversation.id)
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                HorizontalDivider()
                NavigationDrawerItem(
                    label = { Text("Disconnect") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onDisconnect()
                    },
                    modifier = Modifier.padding(16.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedTextColor = MaterialTheme.colorScheme.error,
                        unselectedIconColor = MaterialTheme.colorScheme.error
                    )
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Antigravity") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                    IconButton(onClick = onNewConversation) {
                        Icon(Icons.Default.Add, contentDescription = "New Chat")
                    }
                    val icon = if (uiState.connectionState == BridgeWebSocket.ConnectionState.CONNECTED) {
                        Icons.Default.Wifi
                    } else {
                        Icons.Default.WifiOff
                    }
                    val tint = if (uiState.connectionState == BridgeWebSocket.ConnectionState.CONNECTED) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                    Icon(icon, contentDescription = "Connection", tint = tint, modifier = Modifier.padding(end = 16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            MessageInput(
                isGenerating = uiState.isGenerating,
                onSend = onSendPrompt,
                onCancel = onCancel
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.connectionState != BridgeWebSocket.ConnectionState.CONNECTED) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Reconnecting...",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 16.dp)
                    )
                }
            }
            
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(uiState.steps) { step ->
                    when (step.stepCase) {
                        StepCase.USER_INPUT, StepCase.TEXT, StepCase.MARKDOWN_CHUNK -> {
                            ChatBubble(step = step)
                        }
                        StepCase.PLANNER_RESPONSE -> {
                            val isLatest = uiState.isGenerating && step == uiState.steps.last()
                            ThinkingBubble(step = step, isLatest = isLatest)
                        }
                        StepCase.TOOL_CALL -> {
                            ToolCallCard(
                                step = step,
                                onAcceptEdits = onAcceptEdits,
                                onRejectEdits = onRejectEdits
                            )
                        }
                        else -> {
                            // Ignored steps (e.g. SYSTEM_MESSAGE)
                        }
                    }
                }
                
                if (uiState.isGenerating) {
                    item {
                        TypingIndicator()
                    }
                }
            }
        }
    }
}
}
