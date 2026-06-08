package dev.saifmukhtar.antimatter.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.saifmukhtar.antimatter.network.BridgeWebSocket
import dev.saifmukhtar.antimatter.network.StepCase
import dev.saifmukhtar.antimatter.network.TrajectoryStep
import dev.saifmukhtar.antimatter.viewmodel.ChatUiState
import dev.saifmukhtar.antimatter.ui.components.*
import io.noties.markwon.Markwon

sealed class ChatItem {
    data class SingleStep(val step: TrajectoryStep) : ChatItem()
    data class ThoughtGroup(val steps: List<TrajectoryStep>) : ChatItem()
}

fun groupSteps(steps: List<TrajectoryStep>): List<ChatItem> {
    val result = mutableListOf<ChatItem>()
    var currentGroup = mutableListOf<TrajectoryStep>()
    for (step in steps) {
        if (step.stepCase == StepCase.PLANNER_RESPONSE) {
            currentGroup.add(step)
        } else {
            if (currentGroup.isNotEmpty()) {
                result.add(ChatItem.ThoughtGroup(currentGroup))
                currentGroup = mutableListOf<TrajectoryStep>()
            }
            result.add(ChatItem.SingleStep(step))
        }
    }
    if (currentGroup.isNotEmpty()) {
        result.add(ChatItem.ThoughtGroup(currentGroup))
    }
    return result
}

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
    onChangeModel: () -> Unit,
    onDisconnect: () -> Unit
) {
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val markwon = remember { Markwon.create(context) }

    var isInitialLoad by remember { mutableStateOf(true) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Reset initial load flag when conversation changes
    LaunchedEffect(uiState.conversationId) {
        isInitialLoad = true
    }

    val groupedItems = remember(uiState.steps) { groupSteps(uiState.steps) }
    val totalItemsCount = groupedItems.size + if (uiState.isGenerating) 1 else 0

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(uiState.steps.size) {
        if (totalItemsCount == 0) return@LaunchedEffect
        if (isInitialLoad) {
            listState.scrollToItem(totalItemsCount - 1)
            if (uiState.steps.size >= uiState.expectedStepCount) {
                isInitialLoad = false
            }
        } else {
            val visibleInfo = listState.layoutInfo.visibleItemsInfo
            if (visibleInfo.isNotEmpty()) {
                val lastVisibleIndex = visibleInfo.last().index
                if (lastVisibleIndex >= totalItemsCount - 3) {
                    listState.animateScrollToItem(totalItemsCount - 1)
                }
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
                        TextButton(onClick = onChangeModel) {
                            Text(
                                text = uiState.currentModel,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
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
            
            if (uiState.error != null) {
                Surface(
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = uiState.error,
                        color = MaterialTheme.colorScheme.onError,
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
                contentPadding = PaddingValues(vertical = 16.dp),
                reverseLayout = false
            ) {
                itemsIndexed(
                    items = groupedItems,
                    key = { index, _ -> index }
                ) { _, item ->
                    when (item) {
                        is ChatItem.SingleStep -> {
                            val step = item.step
                            when (step.stepCase) {
                                StepCase.USER_INPUT, StepCase.TEXT, StepCase.MARKDOWN_CHUNK -> {
                                    ChatBubble(step = step)
                                }
                                StepCase.TOOL_CALL -> {
                                    ToolCallCard(
                                        step = step,
                                        onAcceptEdits = onAcceptEdits,
                                        onRejectEdits = onRejectEdits
                                    )
                                }
                                else -> {}
                            }
                        }
                        is ChatItem.ThoughtGroup -> {
                            val isLatest = uiState.isGenerating && item === groupedItems.last()
                            ThoughtGroupBubble(steps = item.steps, isLatest = isLatest)
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
