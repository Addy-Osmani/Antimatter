package dev.saifmukhtar.antimatter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiFind
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.saifmukhtar.antimatter.network.BridgeWebSocket

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectScreen(
    connectionState: BridgeWebSocket.ConnectionState,
    savedUrl: String? = null,
    onConnectClick: (String) -> Unit
) {
    var ipAddress by remember { mutableStateOf(savedUrl ?: "") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Antimatter") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Wifi,
                contentDescription = "Connection",
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Connect to Bridge",
                style = MaterialTheme.typography.headlineMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = when (connectionState) {
                    BridgeWebSocket.ConnectionState.DISCONNECTED -> "Disconnected. Enter LocalTunnel subdomain or IP."
                    BridgeWebSocket.ConnectionState.CONNECTING -> "Connecting..."
                    BridgeWebSocket.ConnectionState.CONNECTED -> "Connected!"
                    else -> ""
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Manual Connection", 
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    OutlinedTextField(
                        value = ipAddress,
                        onValueChange = { ipAddress = it },
                        label = { Text("Subdomain or IP Address") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = {
                            val cleanIp = ipAddress.trim()
                            var url = cleanIp
                            if (!url.startsWith("ws://") && !url.startsWith("wss://")) {
                                if (url.matches(Regex("^[0-9]+\\\\.[0-9]+\\\\.[0-9]+\\\\.[0-9]+(:[0-9]+)?$"))) {
                                    url = "ws://$url"
                                } else {
                                    url = "wss://$url.loca.lt"
                                }
                            }
                            onConnectClick(url)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = connectionState != BridgeWebSocket.ConnectionState.CONNECTING
                    ) {
                        Text("Connect")
                    }
                }
            }
        }
    }
}
