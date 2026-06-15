package dev.saifmukhtar.antimatter.feature.connect

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.saifmukhtar.antimatter.core.network.BridgeWebSocket
import dev.saifmukhtar.antimatter.core.data.GatewayProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectScreen(
    connectionState: BridgeWebSocket.ConnectionState,
    savedUrl: String? = null,
    savedClientId: String? = null,
    savedClientSecret: String? = null,
    profiles: List<GatewayProfile> = emptyList(),
    onConnectClick: (String, String?, String?) -> Unit,
    onScanQRClick: () -> Unit,
    onProfileSelected: (String) -> Unit,
    onProfileDeleted: (String) -> Unit
) {
    var ipAddress by remember { mutableStateOf(savedUrl ?: "") }
    var pairingToken by remember { mutableStateOf("") }
    var clientId by remember { mutableStateOf(savedClientId ?: "") }
    var clientSecret by remember { mutableStateOf(savedClientSecret ?: "") }
    var showAdvanced by remember { mutableStateOf(clientId.isNotBlank() || clientSecret.isNotBlank()) }
    var expanded by remember { mutableStateOf(false) }

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
                .padding(24.dp)
                .verticalScroll(androidx.compose.foundation.rememberScrollState()),
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
                text = "Connect to Gateway",
                style = MaterialTheme.typography.headlineMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = when (connectionState) {
                    BridgeWebSocket.ConnectionState.DISCONNECTED -> "Select a profile or scan a new QR code."
                    BridgeWebSocket.ConnectionState.CONNECTING -> "Connecting..."
                    BridgeWebSocket.ConnectionState.CONNECTED -> "Connected!"
                    else -> ""
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            if (profiles.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = "Saved Profiles (${profiles.size})",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        profiles.forEach { profile ->
                            DropdownMenuItem(
                                text = { Text(profile.name) },
                                onClick = {
                                    expanded = false
                                    onProfileSelected(profile.id)
                                    // Also auto-connect? Yes, triggering state change will auto-connect in ViewModel
                                },
                                trailingIcon = {
                                    IconButton(onClick = { onProfileDeleted(profile.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Profile")
                                    }
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            Button(
                onClick = onScanQRClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan New Pairing QR", style = MaterialTheme.typography.titleMedium)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("OR", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            Spacer(modifier = Modifier.height(24.dp))
            
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
                        "Manual Override", 
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    OutlinedTextField(
                        value = ipAddress,
                        onValueChange = { ipAddress = it },
                        label = { Text("Cloudflare URL (wss://...)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = pairingToken,
                        onValueChange = { pairingToken = it },
                        label = { Text("Pairing Token") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = {
                            var url = ipAddress.trim()
                            if (!url.startsWith("ws://") && !url.startsWith("wss://")) {
                                url = "wss://$url"
                            }
                            if (pairingToken.isNotBlank()) {
                                url = "$url?token=${pairingToken.trim()}"
                            }
                            onConnectClick(url, clientId.trim(), clientSecret.trim())
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
