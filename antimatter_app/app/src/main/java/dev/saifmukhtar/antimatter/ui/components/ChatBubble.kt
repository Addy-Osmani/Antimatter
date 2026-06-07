package dev.saifmukhtar.antimatter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.saifmukhtar.antimatter.network.StepCase
import dev.saifmukhtar.antimatter.network.TrajectoryStep
import io.noties.markwon.Markwon
import androidx.compose.ui.graphics.toArgb

@Composable
fun ChatBubble(
    step: TrajectoryStep
) {
    val isUser = step.stepCase == StepCase.USER_INPUT
    val textColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            // Spacer for AI avatar if needed
            Spacer(modifier = Modifier.width(8.dp))
        }

        Box(
            modifier = Modifier
                .weight(1f, fill = false)
                .background(
                    color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .padding(12.dp)
        ) {
            if (isUser) {
                Text(
                    text = step.displayText,
                    color = textColor,
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                // For AI responses, render Markdown
                MarkdownText(
                    markdown = step.displayText,
                    textColor = textColor.toArgb()
                )
            }
        }
        
        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}
