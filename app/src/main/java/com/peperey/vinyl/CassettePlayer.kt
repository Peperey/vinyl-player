package com.peperey.vinyl

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.sin

// Velocidad de giro de los carretes, en grados por segundo
private const val REEL_SPEED = 180f

@Composable
fun CassettePlayer(
    isPlaying: Boolean,
    label: String,
    progress: Float, // 0f = inicio de la canción, 1f = final
    modifier: Modifier = Modifier
) {
    var angle by remember { mutableFloatStateOf(0f) }

    // Gira los carretes solo mientras suena
    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        var lastFrame = 0L
        while (isActive) {
            withFrameNanos { now ->
                if (lastFrame != 0L) {
                    val seconds = (now - lastFrame) / 1_000_000_000f
                    angle = (angle + seconds * REEL_SPEED) % 360f
                }
                lastFrame = now
            }
        }
    }

    Canvas(modifier = modifier.aspectRatio(1.6f)) {
        val w = size.width
        val h = size.height

        // Cuerpo del casete
        drawRoundRect(
            color = Color(0xFF2E2E34),
            cornerRadius = CornerRadius(w * 0.04f)
        )

        // Etiqueta con el título
        drawRoundRect(
            color = Color(0xFFF3EBD3),
            topLeft = Offset(w * 0.1f, h * 0.08f),
            size = Size(w * 0.8f, h * 0.28f),
            cornerRadius = CornerRadius(w * 0.01f)
        )
        val paint = Paint().apply {
            color = android.graphics.Color.rgb(40, 40, 40)
            textSize = h * 0.07f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
        }
        drawContext.canvas.nativeCanvas.drawText(
            label.take(18), w * 0.5f, h * 0.24f, paint
        )

        // Ventana donde se ven los carretes
        drawRoundRect(
            color = Color(0xFF0E0E10),
            topLeft = Offset(w * 0.18f, h * 0.42f),
            size = Size(w * 0.64f, h * 0.34f),
            cornerRadius = CornerRadius(w * 0.02f)
        )

        // Carretes: el izquierdo se vacía y el derecho se llena
        val rMin = h * 0.06f
        val rMax = h * 0.16f
        val rHub = h * 0.05f
        val leftR = rMax - (rMax - rMin) * progress
        val rightR = rMin + (rMax - rMin) * progress
        val leftC = Offset(w * 0.33f, h * 0.59f)
        val rightC = Offset(w * 0.67f, h * 0.59f)

        // Cinta enrollada
        drawCircle(color = Color(0xFF5A3A1A), radius = leftR, center = leftC)
        drawCircle(color = Color(0xFF5A3A1A), radius = rightR, center = rightC)

        // Centros que giran
        drawReelHub(leftC, rHub, angle)
        drawReelHub(rightC, rHub, angle)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawReelHub(
    center: Offset,
    radius: Float,
    angle: Float
) {
    drawCircle(color = Color(0xFFD8D8D8), radius = radius, center = center)
    for (k in 0 until 3) {
        val a = Math.toRadians((angle + k * 120f).toDouble())
        drawLine(
            color = Color(0xFF444444),
            start = center,
            end = Offset(
                center.x + (cos(a) * radius).toFloat(),
                center.y + (sin(a) * radius).toFloat()
            ),
            strokeWidth = radius * 0.35f
        )
    }
}
