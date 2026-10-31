package com.mayuri.ai.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mayuri.ai.core.voice.VoiceState

@Composable
fun MayuriGlowingOrb(
    voiceState: VoiceState,
    amplitude: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbPulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbRotation"
    )

    // Dynamic color variations based on assistant cognitive state
    val colors = when (voiceState) {
        is VoiceState.Listening -> listOf(Color(0xFF00E5FF), Color(0xFF7C4DFF), Color(0xFF18FFFF))
        is VoiceState.Thinking -> listOf(Color(0xFFFF007F), Color(0xFF7C4DFF), Color(0xFF9C27B0))
        is VoiceState.Speaking -> listOf(Color(0xFF00E676), Color(0xFF00B0FF), Color(0xFF651FFF))
        else -> listOf(Color(0xFF6200EA), Color(0xFF304FFE), Color(0xFF00B0FF))
    }

    Box(modifier = modifier.size(240.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val baseRadius = (size.minDimension / 3.2f) * if (voiceState is VoiceState.Listening) {
                1f + (amplitude * 0.4f)
            } else {
                pulseScale
            }

            // Outer Radial Aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(colors[0].copy(alpha = 0.4f), Color.Transparent),
                    center = center,
                    radius = baseRadius * 1.6f
                ),
                radius = baseRadius * 1.6f,
                center = center
            )

            // Inner Glowing Core
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = colors,
                    center = center
                ),
                radius = baseRadius,
                center = center
            )

            // Dynamic Highlight Center
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.8f), Color.Transparent),
                    center = Offset(center.x - baseRadius * 0.3f, center.y - baseRadius * 0.3f),
                    radius = baseRadius * 0.6f
                ),
                radius = baseRadius * 0.5f,
                center = center
            )
        }
    }
}