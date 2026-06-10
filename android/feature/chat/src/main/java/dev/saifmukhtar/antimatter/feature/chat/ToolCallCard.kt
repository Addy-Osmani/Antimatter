package dev.saifmukhtar.antimatter.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.remember
import dev.saifmukhtar.antimatter.core.network.TrajectoryStep
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import dev.saifmukhtar.antimatter.core.ui.MarkdownText

@Composable
fun ToolCallCard(
    step: TrajectoryStep,
    onAcceptEdits: () -> Unit = {},
    onRejectEdits: () -> Unit = {}
) {
    val parsedArgs = remember(step.value) {
        val stepValue = step.value
        if (!stepValue.isNullOrBlank()) {
            try {
                val jsonObj = org.json.JSONObject(stepValue)
                jsonObj.toString(2)
            } catch (e: Exception) {
                null
            }
        } else null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(vertical = 6.dp, horizontal = 10.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = "Tool",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = step.displayText,
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Monospace
                    )
                }
                
                if (parsedArgs != null && parsedArgs.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    MarkdownText(
                        markdown = "```json\n$parsedArgs\n```",
                        textColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
                    )
                }
                
                if (step.tool == "replace_file_content" || step.tool == "multi_replace_file_content") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(
                            onClick = onAcceptEdits,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Accept Edits")
                        }
                        TextButton(
                            onClick = onRejectEdits,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Reject Edits")
                        }
                    }
                }
            }
        }
    }
}
