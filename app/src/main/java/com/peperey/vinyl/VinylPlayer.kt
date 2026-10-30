package com.peperey.vinyl

import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive

// 33⅓ rpm = 200 grados por segundo
private const val DEGREES_PER_SECOND = 200f

// Ángulo del brazo en reposo y sobre el disco
private const val ARM_REST = -55f
private const val ARM_PLAY = 0f

@Composable
fun VinylPlayer(
    isPlaying: Boolean,
    label: String,
    modifier: Modifier = Modifier
) {
    var discAngle by remember { mutableFloatStateOf(0f) }
    val armAngle = remember { Animatable(ARM_REST) }

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

    // Baja y sube el brazo
    LaunchedEffect(isPlaying) {
        armAngle.animateTo(
            targetValue = if (isPlaying) ARM_PLAY else ARM_REST,
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
            val r = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // Base negra con borde
            drawCircle(color = Color(0xFF0B0B0B), radius = r)
            drawCircle(
                color = Color(0xFF2A2A2A),
                radius = r * 0.985f,
                style = Stroke(width = r * 0.02f)
            )

            // Surcos finos
            for (i in 0 until 60) {
                val frac = 0.36f + i * (0.62f / 60f)
                val alpha = if (i % 3 == 0) 0.22f else 0.07f
                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = r * frac,
                    style = Stroke(width = r * 0.004f)
                )
            }

            // Reflejo que gira con el disco
            drawCircle(
                brush = Brush.sweepGradient(
                    0f to Color.Transparent,
                    0.12f to Color.White.copy(alpha = 0.12f),
                    0.25f to Color.Transparent,
                    0.62f to Color.White.copy(alpha = 0.08f),
                    0.75f to Color.Transparent,
                    1f to Color.Transparent
                ),
                radius = r
            )

            // Etiqueta central con el título
            drawCircle(color = Color(0xFFC62828), radius = r * 0.33f)
            drawCircle(
                color = Color(0xFF8E1B1B),
                radius = r * 0.33f,
                style = Stroke(width = r * 0.01f)
            )

            val paint = Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = r * 0.07f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
                isFakeBoldText = true
            }
            val textY = center.y - (paint.descent() + paint.ascent()) / 2f
            drawContext.canvas.nativeCanvas.drawText(label.take(16), center.x, textY, paint)

            // Agujero central
            drawCircle(color = Color(0xFF888888), radius = r * 0.045f)
            drawCircle(color = Color.Black, radius = r * 0.03f)
        }

        // Brazo (no gira con el disco)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = armAngle.value
                    transformOrigin = TransformOrigin(0.92f, 0.06f)
                }
        ) {
            val w = size.width
            val pivot = Offset(w * 0.92f, w * 0.06f)
            val tip = Offset(w * 0.75f, w * 0.27f)
            val headEnd = Offset(w * 0.735f, w * 0.30f)

            // Base del brazo
            drawCircle(color = Color(0xFF2B2B2B), radius = w * 0.075f, center = pivot)
            drawCircle(color = Color(0xFF9E9E9E), radius = w * 0.05f, center = pivot)
            drawCircle(color = Color(0xFF555555), radius = w * 0.012f, center = pivot)

            // Tubo
            drawLine(
                color = Color(0xFFCFCFCF),
                start = pivot,
                end = tip,
                strokeWidth = w * 0.02f,
                cap = StrokeCap.Round
            )

            // Cabeza
            drawLine(
                color = Color(0xFF3A3A3A),
                start = tip,
                end = headEnd,
                strokeWidth = w * 0.045f,
                cap = StrokeCap.Round
            )

            // Aguja sobre los surcos
            drawLine(
                color = Color(0xFFB0B0B0),
                start = headEnd,
                end = Offset(w * 0.735f, w * 0.31f),
                strokeWidth = w * 0.008f
            )
        }
    }
}

