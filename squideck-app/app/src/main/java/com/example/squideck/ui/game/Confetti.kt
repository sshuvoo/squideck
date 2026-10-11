package com.example.squideck.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.squideck.ui.theme.Amber
import com.example.squideck.ui.theme.MaskWhite
import com.example.squideck.ui.theme.Pink
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private class Piece(
    val x: Float,
    val startY: Float,
    val speed: Float,
    val wobble: Float,
    val phase: Float,
    val spin: Float,
    val width: Float,
    val height: Float,
    val color: Color,
)

private val ConfettiColors = listOf(Pink, MaskWhite, Amber, Color(0xFF2FD3B4))

/** One burst of falling paper for a winning moment; draws nothing once it has fallen. */
@Composable
fun Confetti(modifier: Modifier = Modifier, pieces: Int = 90, durationMs: Int = 2_800) {
    val confetti = remember {
        val random = Random(System.nanoTime())
        List(pieces) {
            Piece(
                x = random.nextFloat(),
                startY = -random.nextFloat() * 0.4f,
                speed = 0.8f + random.nextFloat() * 0.6f,
                wobble = 1f + random.nextFloat() * 2f,
                phase = random.nextFloat(),
                spin = (random.nextFloat() - 0.5f) * 4f,
                width = 6f + random.nextFloat() * 6f,
                height = 10f + random.nextFloat() * 8f,
                color = ConfettiColors[random.nextInt(ConfettiColors.size)],
            )
        }
    }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(durationMs, easing = LinearEasing)) }
    Canvas(modifier) {
        val t = progress.value
        val fade = if (t > 0.8f) (1f - t) / 0.2f else 1f
        confetti.forEach { p ->
            val x = p.x * size.width + sin((t * p.wobble + p.phase) * 2 * PI).toFloat() * 24f
            val y = (p.startY + t * p.speed) * size.height
            rotate(p.spin * t * 360f, Offset(x, y)) {
                drawRect(p.color.copy(alpha = fade), Offset(x - p.width / 2, y - p.height / 2), Size(p.width * density / 2, p.height * density / 2))
            }
        }
    }
}
