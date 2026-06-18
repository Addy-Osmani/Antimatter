package dev.saifmukhtar.antimatter.feature.connect

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.saifmukhtar.antimatter.core.network.BridgeWebSocket
import dev.saifmukhtar.antimatter.core.data.GatewayProfile
import dev.saifmukhtar.antimatter.core.ui.PulseAnimation
import dev.saifmukhtar.antimatter.core.ui.glowBorder
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectScreen(
    connectionState: BridgeWebSocket.ConnectionState,
    savedUrl: String? = null,
    savedClientId: String? = null,
    savedClientSecret: String? = null,
    profiles: List<GatewayProfile> = emptyList(),
    onConnectClick: (url: String, clientId: String?, clientSecret: String?, token: String?) -> Unit,
    onScanQRClick: () -> Unit,
    onProfileSelected: (String) -> Unit,
    onProfileDeleted: (String) -> Unit
) {
    var ipAddress by remember { mutableStateOf(savedUrl ?: "") }
    var pairingToken by remember { mutableStateOf("") }
    var clientId by remember { mutableStateOf(savedClientId ?: "") }
    var clientSecret by remember { mutableStateOf(savedClientSecret ?: "") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(androidx.compose.foundation.rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))
            
            // Hero Section
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
                if (connectionState == BridgeWebSocket.ConnectionState.CONNECTING) {
                    PulseAnimation(
                        modifier = Modifier.size(100.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = "Connecting",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Computer,
                        contentDescription = "Gateway",
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Antimatter Bridge",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = when (connectionState) {
                    BridgeWebSocket.ConnectionState.DISCONNECTED -> "Connect to an agent gateway to begin."
                    BridgeWebSocket.ConnectionState.CONNECTING -> "Establishing secure connection..."
                    BridgeWebSocket.ConnectionState.CONNECTED -> "Connected securely."
                    else -> ""
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))

            // Primary Action
            Button(
                onClick = onScanQRClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .glowBorder(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(20.dp),
                        width = 1.dp,
                        glowRadius = 12.dp
                    ),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Scan Pairing QR", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }

            if (profiles.isNotEmpty()) {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    "Saved Gateways",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(16.dp))

                profiles.forEach { profile ->
                    ElevatedCard(
                        onClick = { onProfileSelected(profile.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Computer,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    profile.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    profile.credentials.url?.removePrefix("wss://") ?: "Unknown",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { onProfileDeleted(profile.id) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete Profile",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Manual Connection Toggle (Optional)
            var showManual by remember { mutableStateOf(false) }
            
            TextButton(onClick = { showManual = !showManual }) {
                Text("Manual Override")
            }
            
            if (showManual) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OutlinedTextField(
                            value = ipAddress,
                            onValueChange = { ipAddress = it },
                            label = { Text("Cloudflare URL") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = pairingToken,
                            onValueChange = { pairingToken = it },
                            label = { Text("Pairing Token") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = {
                                var url = ipAddress.trim()
                                if (!url.startsWith("ws://") && !url.startsWith("wss://")) {
                                    url = "wss://$url"
                                }
                                // Pass the token as a separate argument (sent as Authorization: Bearer header)
                                // rather than appending it to the URL as a query parameter.
                                val token = pairingToken.trim().ifBlank { null }
                                onConnectClick(url, clientId.trim(), clientSecret.trim(), token)
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            enabled = connectionState != BridgeWebSocket.ConnectionState.CONNECTING,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Connect Manually")
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
