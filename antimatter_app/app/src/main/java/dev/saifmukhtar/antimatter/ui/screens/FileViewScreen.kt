package dev.saifmukhtar.antimatter.ui.screens

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
import androidx.compose.ui.text.font.FontFamily
import dev.saifmukhtar.antimatter.ui.components.MarkdownText
import dev.saifmukhtar.antimatter.viewmodel.FilesUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileViewScreen(
    uiState: FilesUiState,
    onBack: () -> Unit,
    onSave: (path: String, content: String) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
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
                }
            )
        },
        floatingActionButton = {
            if (uiState.viewedFileContent != null && !uiState.isLoadingFile) {
                FloatingActionButton(onClick = {
                    if (isEditing) {
                        uiState.viewedFilePath?.let { path ->
                            onSave(path, editedContent)
                        }
                        isEditing = false
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
            if (uiState.isLoadingFile) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.viewedFileContent != null) {
                val markdownCode = """
                    |```${uiState.viewedFileLanguage ?: ""}
                    |${uiState.viewedFileContent}
                    |```
                """.trimMargin()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (isEditing) 0.dp else 16.dp)
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
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                            )
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            MarkdownText(
                                markdown = markdownCode,
                                modifier = Modifier.fillMaxWidth(),
                                textColor = MaterialTheme.colorScheme.onSurface.toArgb()
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
