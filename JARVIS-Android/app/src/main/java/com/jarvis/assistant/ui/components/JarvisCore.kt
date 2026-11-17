package com.jarvis.assistant.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.assistant.ui.theme.JarvisColors
import kotlin.math.cos
import kotlin.math.sin

enum class CoreState {
    IDLE, LISTENING, THINKING, SPEAKING
}

@Composable
fun JarvisCore(
    coreState: CoreState,
    audioLevel: Float,
    modifier: Modifier = Modifier,
    size: Dp = 190.dp
) {
    val transition = rememberInfiniteTransition(label = "jarvis_core")

    val outerRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (coreState) {
                    CoreState.THINKING -> 2600
                    CoreState.LISTENING -> 4200
                    CoreState.SPEAKING -> 5000
                    CoreState.IDLE -> 14000
                },
                easing = LinearEasing
            )
        ),
        label = "outer_rot"
    )

    val innerRotation by transition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (coreState) {
                    CoreState.THINKING -> 1800
                    CoreState.LISTENING -> 3200
                    CoreState.SPEAKING -> 3800
                    CoreState.IDLE -> 10000
                },
                easing = LinearEasing
            )
        ),
        label = "inner_rot"
    )

    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val reactiveBoost by animateFloatAsState(
        targetValue = when (coreState) {
            CoreState.LISTENING -> 1f + audioLevel * 0.25f
            CoreState.SPEAKING -> 1.12f
            CoreState.THINKING -> 1.06f
            CoreState.IDLE -> 1f
        },
        animationSpec = tween(140),
        label = "reactive_boost"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = (this.size.minDimension / 2f) * 0.88f

            // Radial holographic glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        JarvisColors.HoloCyan.copy(alpha = 0.28f),
                        JarvisColors.CoreBlue.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.15f * reactiveBoost
                ),
                center = center,
                radius = baseRadius * 1.15f * reactiveBoost
            )

            // Outer segmented arc ring
            rotate(degrees = outerRotation, pivot = center) {
                val r = baseRadius
                val arcRect = Size(r * 2, r * 2)
                val topLeft = Offset(center.x - r, center.y - r)
                val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)

                for (i in 0 until 4) {
                    drawArc(
                        color = JarvisColors.HoloCyan.copy(alpha = 0.85f),
                        startAngle = i * 90f + 12f,
                        sweepAngle = 54f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcRect,
                        style = stroke
                    )
                }
            }

            // Calibration tick marks
            val tickRadius = baseRadius * 0.83f
            for (angle in 0 until 360 step 15) {
                val rad = Math.toRadians(angle.toDouble())
                val inner = if (angle % 45 == 0) tickRadius - 8.dp.toPx() else tickRadius - 4.dp.toPx()
                drawLine(
                    color = JarvisColors.ArcGlow.copy(alpha = if (angle % 45 == 0) 0.7f else 0.25f),
                    start = Offset(
                        center.x + (inner * cos(rad)).toFloat(),
                        center.y + (inner * sin(rad)).toFloat()
                    ),
                    end = Offset(
                        center.x + (tickRadius * cos(rad)).toFloat(),
                        center.y + (tickRadius * sin(rad)).toFloat()
                    ),
                    strokeWidth = if (angle % 45 == 0) 2.dp.toPx() else 1.dp.toPx()
                )
            }

            // Counter-rotating inner ring
            rotate(degrees = innerRotation, pivot = center) {
                val r = baseRadius * 0.64f
                val arcRect = Size(r * 2, r * 2)
                val topLeft = Offset(center.x - r, center.y - r)
                val stroke = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)

                for (i in 0 until 3) {
                    drawArc(
                        color = JarvisColors.CoreBlue.copy(alpha = 0.95f),
                        startAngle = i * 120f + 18f,
                        sweepAngle = 72f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcRect,
                        style = stroke
                    )
                }
            }

            // Pulsing central reactor ring
            val coreRadius = baseRadius * 0.38f * pulse * reactiveBoost
            drawCircle(
                color = JarvisColors.DarkNavy,
                radius = coreRadius,
                center = center
            )
            drawCircle(
                color = JarvisColors.HoloCyan.copy(alpha = 0.85f),
                radius = coreRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        Text(
            text = when (coreState) {
                CoreState.IDLE -> "JARVIS"
                CoreState.LISTENING -> "LISTEN"
                CoreState.THINKING -> "SYNC"
                CoreState.SPEAKING -> "VOICE"
            },
            color = JarvisColors.TextPrimary,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.sp
        )
    }
}
