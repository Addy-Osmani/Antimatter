package dev.saifmukhtar.antimatter.feature.files

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.ui.text.font.FontFamily
import dev.saifmukhtar.antimatter.core.ui.MarkdownText
import dev.saifmukhtar.antimatter.feature.files.FilesUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileViewScreen(
    uiState: FilesUiState,
    onBack: () -> Unit,
    onSave: (path: String, content: String) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var editedContent by remember(uiState.viewedFileContent) { mutableStateOf(uiState.viewedFileContent ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        uiState.viewedFilePath?.substringAfterLast('/') ?: "File",
                        maxLines = 1
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                    val context = androidx.compose.ui.platform.LocalContext.current
                    if (uiState.viewedFileContent != null) {
                        IconButton(onClick = {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(uiState.viewedFileContent))
                            android.widget.Toast.makeText(context, "Copied file content", android.widget.Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(androidx.compose.material.icons.Icons.Default.ContentCopy, contentDescription = "Copy file")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.viewedFileContent != null && !uiState.isLoadingFile) {
                FloatingActionButton(onClick = {
                    if (isEditing) {
                        showSaveDialog = true
                    } else {
                        isEditing = true
                    }
                }) {
                    Icon(
                        imageVector = if (isEditing) Icons.Default.Save else Icons.Default.Edit,
                        contentDescription = if (isEditing) "Save" else "Edit"
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (showSaveDialog) {
                AlertDialog(
                    onDismissRequest = { showSaveDialog = false },
                    title = { Text("Confirm Save") },
                    text = { Text("Are you sure you want to overwrite this file? This cannot be undone.") },
                    confirmButton = {
                        TextButton(onClick = {
                            uiState.viewedFilePath?.let { path ->
                                onSave(path, editedContent)
                            }
                            isEditing = false
                            showSaveDialog = false
                        }) {
                            Text("Overwrite")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showSaveDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
            if (uiState.isLoadingFile) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.viewedFileContent != null) {
                val isMarkdown = uiState.viewedFileLanguage == "markdown" || uiState.viewedFilePath?.endsWith(".md") == true
                val markdownCode = if (isMarkdown) {
                    uiState.viewedFileContent
                } else {
                    """
                        |```${uiState.viewedFileLanguage ?: ""}
                        |${uiState.viewedFileContent}
                        |```
                    """.trimMargin()
                }

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (isEditing) 0.dp else 16.dp),
                    shape = if (isEditing) androidx.compose.ui.graphics.RectangleShape else androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = if (isEditing) 0.dp else 4.dp
                ) {
                    if (isEditing) {
                        TextField(
                            value = editedContent,
                            onValueChange = { editedContent = it },
                            modifier = Modifier.fillMaxSize(),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                            )
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                        ) {
                            MarkdownText(
                                markdown = markdownCode,
                                textColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "Failed to load file.",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
