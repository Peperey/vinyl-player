package com.peperey.vinyl

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive

// 33⅓ rpm = 200 grados por segundo
private const val DEGREES_PER_SECOND = 200f

@Composable
fun VinylPlayer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    var discAngle by remember { mutableFloatStateOf(0f) }
    val armAngle = remember { Animatable(-25f) }

    // Gira el disco solo mientras suena
    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        var lastFrame = 0L
        while (isActive) {
            withFrameNanos { now ->
                if (lastFrame != 0L) {
                    val seconds = (now - lastFrame) / 1_000_000_000f
                    discAngle = (discAngle + seconds * DEGREES_PER_SECOND) % 360f
                }
                lastFrame = now
            }
        }
    }

    // Mueve el brazo al empezar y al terminar
    LaunchedEffect(isPlaying) {
        armAngle.animateTo(
            targetValue = if (isPlaying) 0f else -25f,
            animationSpec = tween(durationMillis = 700)
        )
    }

    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        // Disco
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = discAngle }
        ) {
            val radius = size.minDimension / 2f

            drawCircle(color = Color(0xFF111111), radius = radius)

            for (i in 0 until 18) {
                drawCircle(
                    color = Color(0xFF2A2A2A),
                    radius = radius * (0.45f + i * 0.028f),
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            drawCircle(color = Color(0xFFC62828), radius = radius * 0.32f)
            drawCircle(color = Color(0xFF111111), radius = radius * 0.04f)
        }

        // Brazo
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = armAngle.value
                    transformOrigin = TransformOrigin(0.85f, 0.1f)
                }
        ) {
            val w = size.width
            val pivot = Offset(w * 0.85f, w * 0.1f)

            drawCircle(color = Color(0xFF888888), radius = w * 0.05f, center = pivot)

            drawLine(
                color = Color(0xFFBBBBBB),
                start = pivot,
                end = Offset(w * 0.55f, w * 0.5f),
                strokeWidth = w * 0.025f,
                cap = StrokeCap.Round
            )

            drawLine(
                color = Color(0xFF444444),
                start = Offset(w * 0.55f, w * 0.5f),
                end = Offset(w * 0.52f, w * 0.56f),
                strokeWidth = w * 0.04f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun PlayerScreen() {
    var isPlaying by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        VinylPlayer(
            isPlaying = isPlaying,
            modifier = Modifier.fillMaxWidth(0.8f)
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = { isPlaying = !isPlaying }) {
            Text(if (isPlaying) "Pausar" else "Reproducir")
        }
    }
}
