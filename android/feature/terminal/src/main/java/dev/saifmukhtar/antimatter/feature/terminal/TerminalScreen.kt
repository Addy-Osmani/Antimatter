package dev.saifmukhtar.antimatter.feature.terminal

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    uiState: TerminalUiState,
    onUnlock: () -> Unit,
    onExecuteCommand: (String) -> Unit,
    onClearTerminal: () -> Unit
) {
    val context = LocalContext.current

    if (!uiState.isUnlocked) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Terminal is Locked",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Authenticate to access remote shell",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = {
                    val activity = context as? FragmentActivity
                    if (activity != null) {
                        val executor = ContextCompat.getMainExecutor(activity)
                        val biometricPrompt = BiometricPrompt(activity, executor,
                            object : BiometricPrompt.AuthenticationCallback() {
                                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                    super.onAuthenticationError(errorCode, errString)
                                    Toast.makeText(context, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                                }
                                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                    super.onAuthenticationSucceeded(result)
                                    onUnlock()
                                }
                                override fun onAuthenticationFailed() {
                                    super.onAuthenticationFailed()
                                    Toast.makeText(context, "Authentication failed", Toast.LENGTH_SHORT).show()
                                }
                            })

                        val promptInfo = BiometricPrompt.PromptInfo.Builder()
                            .setTitle("Unlock Terminal")
                            .setSubtitle("Authenticate to run commands")
                            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                            .build()

                        biometricPrompt.authenticate(promptInfo)
                    } else {
                        Toast.makeText(context, "Activity context required for biometrics", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("Unlock Terminal")
                }
            }
        }
    } else {
        var commandText by remember { mutableStateOf("") }
        val listState = rememberLazyListState()

        LaunchedEffect(uiState.outputLines.size) {
            if (uiState.outputLines.isNotEmpty()) {
                listState.animateScrollToItem(uiState.outputLines.size - 1)
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Remote Terminal") },
                    actions = {
                        IconButton(onClick = onClearTerminal) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Terminal")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF1E1E1E))
                        .padding(8.dp)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.outputLines) { line ->
                            Text(
                                text = line.text,
                                color = if (line.isError) Color(0xFFFF6B6B) else Color(0xFFE0E0E0),
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                
                Surface(
                    tonalElevation = 2.dp,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = commandText,
                            onValueChange = { commandText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Enter command...", fontFamily = FontFamily.Monospace) },
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                            shape = RoundedCornerShape(24.dp),
                            enabled = uiState.isConnected,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (commandText.isNotBlank()) {
                                        onExecuteCommand(commandText)
                                        commandText = ""
                                    }
                                }
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledIconButton(
                            onClick = {
                                if (commandText.isNotBlank()) {
                                    onExecuteCommand(commandText)
                                    commandText = ""
                                }
                            },
                            enabled = uiState.isConnected && commandText.isNotBlank()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Command")
                        }
                    }
                }
            }
        }
    }
}
