package dev.saifmukhtar.antimatter.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.saifmukhtar.antimatter.network.FileNode
import dev.saifmukhtar.antimatter.viewmodel.FilesUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(
    uiState: FilesUiState,
    onRefresh: () -> Unit,
    onOpenFile: (String) -> Unit,
    onCreateNode: (path: String, isDirectory: Boolean) -> Unit
) {
    var expandedFolders by remember { mutableStateOf(setOf<String>()) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newNodeName by remember { mutableStateOf("") }
    var isCreatingDirectory by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (uiState.fileTree == null && !uiState.isLoadingTree) {
            onRefresh()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workspace") },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.fileTree != null && !uiState.isLoadingTree) {
                FloatingActionButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Create File/Folder")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoadingTree) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.fileTree.isNullOrEmpty()) {
                Text(
                    text = "No files found or not connected to workspace.",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(uiState.fileTree) { node ->
                        FileTreeNode(
                            node = node,
                            depth = 0,
                            expandedFolders = expandedFolders,
                            onToggleFolder = { path ->
                                expandedFolders = if (expandedFolders.contains(path)) {
                                    expandedFolders - path
                                } else {
                                    expandedFolders + path
                                }
                            },
                            onOpenFile = onOpenFile
                        )
                    }
                }
            }
        }

        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text("Create New Node") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newNodeName,
                            onValueChange = { newNodeName = it },
                            label = { Text("Name (include path/ if needed)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = !isCreatingDirectory,
                                onClick = { isCreatingDirectory = false }
                            )
                            Text("File", modifier = Modifier.clickable { isCreatingDirectory = false })
                            Spacer(modifier = Modifier.width(16.dp))
                            RadioButton(
                                selected = isCreatingDirectory,
                                onClick = { isCreatingDirectory = true }
                            )
                            Text("Directory", modifier = Modifier.clickable { isCreatingDirectory = true })
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (newNodeName.isNotBlank()) {
                            onCreateNode(newNodeName.trim(), isCreatingDirectory)
                            newNodeName = ""
                            showCreateDialog = false
                            // Refresh triggered automatically after success but maybe wait a bit?
                        }
                    }) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun FileTreeNode(
    node: FileNode,
    depth: Int,
    expandedFolders: Set<String>,
    onToggleFolder: (String) -> Unit,
    onOpenFile: (String) -> Unit
) {
    val isExpanded = expandedFolders.contains(node.path)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (node.isDir) {
                    onToggleFolder(node.path)
                } else {
                    onOpenFile(node.path)
                }
            }
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .padding(start = (depth * 16).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (node.isDir) {
                if (isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder
            } else {
                Icons.Default.Description
            },
            contentDescription = null,
            tint = if (node.isDir) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = node.name,
            style = MaterialTheme.typography.bodyLarge
        )
    }

    if (node.isDir && isExpanded && node.children != null) {
        node.children.forEach { child ->
            FileTreeNode(
                node = child,
                depth = depth + 1,
                expandedFolders = expandedFolders,
                onToggleFolder = onToggleFolder,
                onOpenFile = onOpenFile
            )
        }
    }
}
