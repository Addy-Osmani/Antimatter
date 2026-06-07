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
import dev.saifmukhtar.antimatter.ui.components.MarkdownText
import dev.saifmukhtar.antimatter.viewmodel.FilesUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileViewScreen(
    uiState: FilesUiState,
    onBack: () -> Unit
) {
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
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    MarkdownText(
                        markdown = markdownCode,
                        modifier = Modifier.fillMaxWidth(),
                        textColor = MaterialTheme.colorScheme.onSurface.toArgb()
                    )
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
