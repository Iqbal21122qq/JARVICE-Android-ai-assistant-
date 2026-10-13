package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AlertRed
import com.example.ui.theme.ArcBlue
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.HologramTeal
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArcReactorVisualizer(
    isListening: Boolean,
    isSpeaking: Boolean,
    amplitude: Float,
    isRooted: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ArcReactorRotation")

    val clockwiseAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Clockwise"
    )

    val counterClockwiseAngle by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "CounterClockwise"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    val activePrimaryColor = when {
        isListening -> ArcCyan
        isSpeaking -> HologramTeal
        !isRooted -> AlertRed
        else -> ArcBlue
    }

    Box(
        modifier = modifier.size(170.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = size.minDimension / 2f * 0.92f

            // Dynamic amplitude expand factor
            val ampBoost = if (isListening || isSpeaking) amplitude * 0.15f else 0f
            val dynamicRadius = (baseRadius * pulseScale) * (1f + ampBoost)

            // Outer Ambient Glow Ring
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(activePrimaryColor.copy(alpha = 0.25f), Color.Transparent),
                    center = center,
                    radius = dynamicRadius
                ),
                radius = dynamicRadius,
                center = center
            )

            // Outer segmented ring (Counter-Clockwise)
            rotate(counterClockwiseAngle, pivot = center) {
                val segCount = 12
                val segAngle = 360f / segCount
                for (i in 0 until segCount) {
                    val startAngle = i * segAngle
                    drawArc(
                        color = activePrimaryColor.copy(alpha = 0.7f),
                        startAngle = startAngle,
                        sweepAngle = segAngle * 0.65f,
                        useCenter = false,
                        topLeft = Offset(center.x - dynamicRadius * 0.88f, center.y - dynamicRadius * 0.88f),
                        size = androidx.compose.ui.geometry.Size(dynamicRadius * 1.76f, dynamicRadius * 1.76f),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            // Middle ring with ticks (Clockwise)
            rotate(clockwiseAngle, pivot = center) {
                val tickCount = 24
                val angleStep = (2 * Math.PI / tickCount).toFloat()
                val rIn = dynamicRadius * 0.62f
                val rOut = dynamicRadius * 0.74f

                for (i in 0 until tickCount) {
                    val a = i * angleStep
                    val p1 = Offset(center.x + rIn * cos(a), center.y + rIn * sin(a))
                    val p2 = Offset(center.x + rOut * cos(a), center.y + rOut * sin(a))
                    drawLine(
                        color = if (i % 3 == 0) ArcCyan else activePrimaryColor.copy(alpha = 0.4f),
                        start = p1,
                        end = p2,
                        strokeWidth = if (i % 3 == 0) 2.5.dp.toPx() else 1.2.dp.toPx()
                    )
                }
            }

            // Inner Ring
            drawCircle(
                color = ArcCyan.copy(alpha = 0.8f),
                radius = dynamicRadius * 0.48f,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Central Triangular Core
            val triRadius = dynamicRadius * 0.32f
            val triPath = Path().apply {
                val angle0 = -Math.PI / 2
                val angle1 = angle0 + (2 * Math.PI / 3)
                val angle2 = angle0 + (4 * Math.PI / 3)

                moveTo(center.x + (triRadius * cos(angle0)).toFloat(), center.y + (triRadius * sin(angle0)).toFloat())
                lineTo(center.x + (triRadius * cos(angle1)).toFloat(), center.y + (triRadius * sin(angle1)).toFloat())
                lineTo(center.x + (triRadius * cos(angle2)).toFloat(), center.y + (triRadius * sin(angle2)).toFloat())
                close()
            }

            rotate(counterClockwiseAngle * 0.5f, pivot = center) {
                drawPath(
                    path = triPath,
                    color = activePrimaryColor.copy(alpha = 0.45f)
                )
                drawPath(
                    path = triPath,
                    color = Color.White.copy(alpha = 0.9f),
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // Central Glowing Core Orb
            val coreOrbRadius = dynamicRadius * 0.16f * (1f + amplitude * 0.5f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, activePrimaryColor, Color.Transparent),
                    center = center,
                    radius = coreOrbRadius * 1.5f
                ),
                radius = coreOrbRadius * 1.5f,
                center = center
            )
            drawCircle(
                color = Color.White,
                radius = coreOrbRadius * 0.5f,
                center = center
            )
        }
    }
}
