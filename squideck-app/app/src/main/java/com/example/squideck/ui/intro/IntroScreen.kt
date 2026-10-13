package com.example.squideck.ui.intro

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.squideck.ui.theme.Ink
import com.example.squideck.ui.theme.Pink
import com.example.squideck.ui.theme.White
import kotlin.math.min
import kotlin.math.pow

/*
 * Animated intro in the style of the series' title card, drawn entirely in code: the "SQUID / DECK" wordmark is
 * written stroke by stroke like a pen, a pink pen tip leading each line, then a pink circle, triangle and square
 * pop into the Q, D and E. Tap to skip.
 *
 * The wordmark is authored in "letter units": each letter is 1 unit tall, coordinates grow right and down.
 */

private const val STROKE_DELAY_MS = 120f
private const val STROKE_DRAW_MS = 620f
private const val SHAPES_START_MS = 2_150f
private const val SHAPE_STAGGER_MS = 150f
private const val SHAPE_POP_MS = 420f
private const val FADE_OUT_START_MS = 3_450f
private const val TOTAL_MS = 3_900f

private const val STROKE_WIDTH = 0.085f

private class LogoStroke(val path: Path)

private class PathBuilder(private val ox: Float, private val oy: Float) {
    val path = Path()
    fun m(x: Float, y: Float) = path.moveTo(ox + x, oy + y)
    fun l(x: Float, y: Float) = path.lineTo(ox + x, oy + y)
    /** Arc around (cx, cy); angles in degrees, 0 = right, positive = clockwise on screen. */
    fun a(cx: Float, cy: Float, r: Float, startDeg: Float, sweepDeg: Float, jump: Boolean = false) {
        path.arcTo(Rect(ox + cx - r, oy + cy - r, ox + cx + r, oy + cy + r), startDeg, sweepDeg, jump)
    }
}

private fun stroke(ox: Float, oy: Float, block: PathBuilder.() -> Unit) = LogoStroke(PathBuilder(ox, oy).apply(block).path)

// Letter positions: "SQUID" on the first line, "DECK" on the second, shifted right like the original card.
private const val LINE2 = 1.5f
private const val S_X = 0f
private const val Q_X = 1.32f
private const val U_X = 2.64f
private const val I_X = 3.96f
private const val D1_X = 4.48f
private const val D2_X = 0.9f
private const val E_X = 2.22f
private const val C_X = 3.75f
private const val K_X = 5.07f

/** Strokes in writing order, letter by letter: S Q U I D, then D E C K. */
private val Strokes: List<LogoStroke> = listOf(
    stroke(S_X, 0f) { m(1f, 0f); l(0.25f, 0f); a(0.25f, 0.25f, 0.25f, -90f, -180f); l(0.75f, 0.5f); a(0.75f, 0.75f, 0.25f, -90f, 180f); l(-0.55f, 1f) },
    stroke(Q_X, 0f) { a(0.5f, 0.5f, 0.5f, 0f, 359.9f, jump = true) },
    stroke(Q_X, 0f) { m(0.64f, 0.64f); l(1.0f, 1.04f) },
    stroke(U_X, 0f) { m(0f, 0f); l(0f, 0.5f); a(0.5f, 0.5f, 0.5f, 180f, -180f); l(1f, 0f) },
    stroke(I_X, 0f) { m(0.1f, 0f); l(0.1f, 1f) },
    stroke(D1_X, 0f) { m(0f, 0f); l(0f, 1f); l(0.5f, 1f); a(0.5f, 0.5f, 0.5f, 90f, -180f); path.close() },
    stroke(D2_X, LINE2) { m(0f, 0f); l(0f, 1f); l(0.5f, 1f); a(0.5f, 0.5f, 0.5f, 90f, -180f); path.close() },
    stroke(E_X, LINE2) { m(1f, 0f); l(0f, 0f); l(0f, 1f); l(1f, 1f) },
    stroke(E_X, LINE2) { m(0.88f, 0.5f); l(1.3f, 0.5f) },
    stroke(C_X, LINE2) { a(0.5f, 0.5f, 0.5f, 45f, 270f, jump = true) },
    stroke(K_X, LINE2) { m(0f, 0f); l(0f, 1f) },
    stroke(K_X, LINE2) { m(1f, 0f); l(0.12f, 0.5f); l(1f, 1f) },
)

/** Wordmark bounds in letter units, used to centre and scale it to the screen. */
private val LogoBounds = Rect(-0.55f, 0f, K_X + 1f, LINE2 + 1.04f)

private fun easeOutCubic(t: Float) = 1f - (1f - t).pow(3)

/** Overshoots slightly before settling, for the shapes "popping" in. */
private fun easeOutBack(t: Float): Float {
    val c = 1.9f
    return 1f + (c + 1f) * (t - 1f).pow(3) + c * (t - 1f).pow(2)
}

private fun phase(timeMs: Float, startMs: Float, durationMs: Float) = ((timeMs - startMs) / durationMs).coerceIn(0f, 1f)

@Composable
fun IntroScreen(onFinished: () -> Unit, modifier: Modifier = Modifier) {
    val time = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        time.animateTo(TOTAL_MS, tween(TOTAL_MS.toInt(), easing = LinearEasing))
        onFinished()
    }
    Canvas(
        modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onFinished)
            .semantics { contentDescription = "Squideck intro. Tap to skip." },
    ) {
        val t = time.value
        val fade = 1f - phase(t, FADE_OUT_START_MS, TOTAL_MS - FADE_OUT_START_MS)
        drawRect(Ink)
        val unit = min(size.width * 0.78f / LogoBounds.width, size.height * 0.3f / LogoBounds.height)
        val origin = Offset(
            (size.width - LogoBounds.width * unit) / 2f - LogoBounds.left * unit,
            (size.height - LogoBounds.height * unit) / 2f - LogoBounds.top * unit,
        )
        translate(origin.x, origin.y) {
            scale(unit, unit, pivot = Offset.Zero) {
                drawStrokes(t, fade)
                drawShapes(t, fade)
            }
        }
    }
}

/** Length of each stroke, measured once, so the pen can reveal a fraction of it per frame. */
private val StrokeMeasures: List<Pair<PathMeasure, Float>> = Strokes.map { stroke ->
    val measure = PathMeasure().apply { setPath(stroke.path, false) }
    measure to measure.length
}

private fun DrawScope.drawStrokes(t: Float, fade: Float) {
    val line = Stroke(width = STROKE_WIDTH, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val partial = Path()
    Strokes.forEachIndexed { index, stroke ->
        val p = easeInOut(phase(t, index * STROKE_DELAY_MS, STROKE_DRAW_MS))
        if (p <= 0f) return@forEachIndexed
        if (p >= 1f) {
            drawPath(stroke.path, White.copy(alpha = fade), style = line)
            return@forEachIndexed
        }
        // Reveal the stroke from its start up to the pen position.
        val (measure, length) = StrokeMeasures[index]
        partial.reset()
        measure.getSegment(0f, length * p, partial, true)
        drawPath(partial, White.copy(alpha = fade), style = line)
        // The pen tip: a pink dot with a soft glow, riding the end of the line.
        val tip = measure.getPosition(length * p)
        drawCircle(Pink.copy(alpha = 0.35f * fade), STROKE_WIDTH * 1.8f, tip)
        drawCircle(Pink.copy(alpha = fade), STROKE_WIDTH * 0.75f, tip)
    }
}

/** Starts and ends gently, like a hand drawing a line. */
private fun easeInOut(t: Float) = if (t < 0.5f) 2f * t * t else 1f - (-2f * t + 2f).pow(2) / 2f

private fun DrawScope.drawShapes(t: Float, fade: Float) {
    drawLogoShapes(
        circle = easeOutBack(phase(t, SHAPES_START_MS, SHAPE_POP_MS)),
        triangle = easeOutBack(phase(t, SHAPES_START_MS + SHAPE_STAGGER_MS, SHAPE_POP_MS)),
        square = easeOutBack(phase(t, SHAPES_START_MS + SHAPE_STAGGER_MS * 2, SHAPE_POP_MS)),
        color = Pink.copy(alpha = fade),
    )
}

/** Pink circle in the Q, triangle in the second D and square in the E, each scaled 0..1 (with overshoot). */
private fun DrawScope.drawLogoShapes(circle: Float, triangle: Float, square: Float, color: Color) {
    if (circle > 0f) drawCircle(color, 0.26f * circle, Offset(Q_X + 0.5f, 0.5f))
    if (triangle > 0f) {
        val c = Offset(D2_X + 0.36f, LINE2 + 0.5f)
        val tri = Path().apply {
            moveTo(c.x - 0.2f * triangle, c.y - 0.28f * triangle)
            lineTo(c.x + 0.3f * triangle, c.y)
            lineTo(c.x - 0.2f * triangle, c.y + 0.28f * triangle)
            close()
        }
        drawPath(tri, color)
    }
    if (square > 0f) {
        val half = 0.24f * square
        val c = Offset(E_X + 0.5f, LINE2 + 0.5f)
        drawRect(color, Offset(c.x - half, c.y - half), Size(half * 2, half * 2))
    }
}

/** The finished SQUID / DECK wordmark as a still logo, fitted to its box (used on the invitation card). */
@Composable
fun SquideckLogo(modifier: Modifier = Modifier, lineColor: Color = White, shapeColor: Color = Pink) {
    Canvas(modifier.semantics { contentDescription = "Squideck" }) {
        val unit = min(size.width / LogoBounds.width, size.height / LogoBounds.height)
        val origin = Offset(
            (size.width - LogoBounds.width * unit) / 2f - LogoBounds.left * unit,
            (size.height - LogoBounds.height * unit) / 2f - LogoBounds.top * unit,
        )
        translate(origin.x, origin.y) {
            scale(unit, unit, pivot = Offset.Zero) {
                val line = Stroke(width = STROKE_WIDTH * 1.4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                Strokes.forEach { drawPath(it.path, lineColor, style = line) }
                drawLogoShapes(1f, 1f, 1f, shapeColor)
            }
        }
    }
}
