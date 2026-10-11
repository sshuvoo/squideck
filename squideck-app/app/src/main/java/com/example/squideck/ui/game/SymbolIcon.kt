package com.example.squideck.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.squideck.domain.Symbol
import com.example.squideck.ui.theme.MaskWhite
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Outline glyph for a symbol, white like the guards' masks; `null` draws the Imposter mask. */
@Composable
fun SymbolIcon(symbol: Symbol?, modifier: Modifier = Modifier, color: Color = MaskWhite) {
    Canvas(modifier) {
        val w = size.minDimension
        val stroke = Stroke(width = w * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (symbol) {
            Symbol.Star -> {
                val center = Offset(w / 2, w / 2 + w * 0.04f)
                val outer = w * 0.46f
                val inner = outer * 0.42f
                val path = Path()
                for (i in 0 until 10) {
                    val angle = -PI / 2 + i * PI / 5
                    val radius = if (i % 2 == 0) outer else inner
                    val x = center.x + (radius * cos(angle)).toFloat()
                    val y = center.y + (radius * sin(angle)).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                drawPath(path, color, style = stroke)
            }

            Symbol.Square -> drawRoundRect(
                color, Offset(w * 0.12f, w * 0.12f), Size(w * 0.76f, w * 0.76f),
                CornerRadius(w * 0.08f), style = stroke,
            )

            Symbol.Triangle -> {
                val path = Path().apply {
                    moveTo(w * 0.5f, w * 0.1f)
                    lineTo(w * 0.92f, w * 0.86f)
                    lineTo(w * 0.08f, w * 0.86f)
                    close()
                }
                drawPath(path, color, style = stroke)
            }

            Symbol.Circle -> drawCircle(color, radius = w * 0.4f, style = stroke)

            Symbol.Umbrella -> {
                drawArc(
                    color, 180f, 180f, useCenter = true,
                    topLeft = Offset(w * 0.1f, w * 0.15f), size = Size(w * 0.8f, w * 0.8f), style = stroke,
                )
                val handle = Path().apply {
                    moveTo(w * 0.5f, w * 0.55f)
                    lineTo(w * 0.5f, w * 0.8f)
                    cubicTo(w * 0.5f, w * 0.94f, w * 0.34f, w * 0.94f, w * 0.34f, w * 0.8f)
                }
                drawPath(handle, color, style = stroke)
            }

            null -> {
                // Imposter: a mask with slanted eyes and a sly zigzag grin.
                val mask = Path().apply {
                    moveTo(w * 0.15f, w * 0.2f)
                    quadraticTo(w * 0.5f, w * 0.1f, w * 0.85f, w * 0.2f)
                    lineTo(w * 0.85f, w * 0.5f)
                    cubicTo(w * 0.85f, w * 0.75f, w * 0.65f, w * 0.9f, w * 0.5f, w * 0.9f)
                    cubicTo(w * 0.35f, w * 0.9f, w * 0.15f, w * 0.75f, w * 0.15f, w * 0.5f)
                    close()
                }
                drawPath(mask, color, style = stroke)
                drawLine(color, Offset(w * 0.27f, w * 0.4f), Offset(w * 0.43f, w * 0.5f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.73f, w * 0.4f), Offset(w * 0.57f, w * 0.5f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
                val grin = Path().apply {
                    moveTo(w * 0.35f, w * 0.72f)
                    lineTo(w * 0.43f, w * 0.65f)
                    lineTo(w * 0.5f, w * 0.72f)
                    lineTo(w * 0.57f, w * 0.65f)
                    lineTo(w * 0.65f, w * 0.72f)
                }
                drawPath(grin, color, style = Stroke(w * 0.06f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}
