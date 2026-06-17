package dev.saifmukhtar.antimatter.core.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun PulseAnimation(
    modifier: Modifier = Modifier,
    color: Color,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }
                .background(color, shape = androidx.compose.foundation.shape.CircleShape)
        )
        content()
    }
}

fun Modifier.glowBorder(
    color: Color,
    shape: Shape = RoundedCornerShape(8.dp),
    width: Dp = 2.dp,
    glowRadius: Dp = 8.dp
): Modifier = this.composed {
    val infiniteTransition = rememberInfiniteTransition(label = "glowPulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    
    this.drawWithCache {
        onDrawWithContent {
            drawContent()
            val outline = shape.createOutline(size, layoutDirection, this)
            val path = androidx.compose.ui.graphics.Path().apply {
                when (outline) {
                    is androidx.compose.ui.graphics.Outline.Rectangle -> addRect(outline.rect)
                    is androidx.compose.ui.graphics.Outline.Rounded -> addRoundRect(outline.roundRect)
                    is androidx.compose.ui.graphics.Outline.Generic -> addPath(outline.path)
                }
            }
            drawPath(
                path = path,
                color = color.copy(alpha = alpha),
                style = Stroke(width = width.toPx())
            )
            // A simple glow approximation by drawing a thicker, more transparent stroke
            drawPath(
                path = path,
                color = color.copy(alpha = alpha * 0.3f),
                style = Stroke(width = glowRadius.toPx())
            )
        }
    }
}

fun Modifier.shimmerEffect(): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )
    
    val colors = listOf(
        Color.Transparent,
        Color.White.copy(alpha = 0.2f),
        Color.Transparent
    )

    this.drawWithCache {
        val brush = Brush.linearGradient(
            colors = colors,
            start = Offset(offset - 200f, offset - 200f),
            end = Offset(offset + 200f, offset + 200f)
        )
        onDrawWithContent {
            drawContent()
            drawRect(
                brush = brush,
                blendMode = BlendMode.SrcAtop
            )
        }
    }
}
