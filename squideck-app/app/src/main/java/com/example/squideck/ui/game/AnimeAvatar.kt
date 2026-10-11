package com.example.squideck.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import com.example.squideck.ui.theme.Pink

enum class HairStyle { Long, Short, Spiky, TwinTails }
enum class Outfit { Sailor, Gakuran }

data class AvatarStyle(
    val hairStyle: HairStyle,
    val outfit: Outfit,
    val hair: Color,
    val eyes: Color,
    val background: Color,
    val skin: Color = Color(0xFFFFE4D3),
)

/** One look per bot seat; names live with the session so avatars stay stable between rounds. */
object AvatarStyles {
    private val all = listOf(
        AvatarStyle(HairStyle.Long, Outfit.Sailor, Color(0xFF1B1530), Color(0xFF8E5BFF), Color(0xFF2A6B60)),
        AvatarStyle(HairStyle.Spiky, Outfit.Gakuran, Color(0xFF1F2A44), Color(0xFFFFB02E), Color(0xFF5B2A3A)),
        AvatarStyle(HairStyle.TwinTails, Outfit.Sailor, Color(0xFF6B3A2E), Color(0xFF2FC47A), Color(0xFF1F3C5A)),
        AvatarStyle(HairStyle.Short, Outfit.Gakuran, Color(0xFF2B2420), Color(0xFF4FA3FF), Color(0xFF3A4A2A)),
    )

    fun forSeat(seat: Int): AvatarStyle = all[(seat - 1).mod(all.size)]
}

private val Ink = Color(0xFF14101A)
private val Navy = Color(0xFF1D2B53)
private val Paper = Color(0xFFF6F4F8)
private val SkinShadeAlpha = 0.18f

/**
 * Anime-style bust drawn in code. Everything is authored in a 0..1 unit square and scaled to the
 * canvas, so avatars stay crisp at any size and need no image assets.
 */
@Composable
fun AnimeAvatar(style: AvatarStyle, modifier: Modifier = Modifier) {
    Canvas(modifier.clip(CircleShape)) {
        withTransform({ scale(size.width, size.height, Offset.Zero) }) {
            drawRect(
                Brush.verticalGradient(listOf(lerp(style.background, Color.White, 0.12f), style.background, lerp(style.background, Color.Black, 0.35f)), 0f, 1f),
                size = Size(1f, 1f),
            )
            drawBackHair(style)
            drawOutfit(style)
            drawFace(style)
            drawFrontHair(style)
            drawEye(style, cx = 0.365f)
            drawEye(style, cx = 0.635f)
            drawNoseAndMouth()
        }
    }
}

private fun path(block: Path.() -> Unit) = Path().apply(block)

private fun DrawScope.drawBackHair(style: AvatarStyle) {
    val dark = lerp(style.hair, Color.Black, 0.3f)
    when (style.hairStyle) {
        HairStyle.Long -> drawPath(
            path {
                moveTo(0.13f, 0.42f)
                cubicTo(0.08f, 0.05f, 0.92f, 0.05f, 0.87f, 0.42f)
                lineTo(0.92f, 0.98f)
                cubicTo(0.7f, 1.04f, 0.3f, 1.04f, 0.08f, 0.98f)
                close()
            },
            dark,
        )
        HairStyle.TwinTails -> {
            drawPath(path {
                moveTo(0.2f, 0.3f); cubicTo(-0.04f, 0.4f, -0.02f, 0.82f, 0.1f, 1.0f)
                cubicTo(0.2f, 0.86f, 0.28f, 0.62f, 0.27f, 0.44f); close()
            }, dark)
            drawPath(path {
                moveTo(0.8f, 0.3f); cubicTo(1.04f, 0.4f, 1.02f, 0.82f, 0.9f, 1.0f)
                cubicTo(0.8f, 0.86f, 0.72f, 0.62f, 0.73f, 0.44f); close()
            }, dark)
            drawCircle(Pink, 0.035f, Offset(0.19f, 0.33f))
            drawCircle(Pink, 0.035f, Offset(0.81f, 0.33f))
        }
        HairStyle.Short, HairStyle.Spiky -> Unit
    }
}

private fun DrawScope.drawOutfit(style: AvatarStyle) {
    val shoulders = path {
        moveTo(-0.02f, 1.02f); lineTo(0f, 0.9f)
        cubicTo(0.1f, 0.8f, 0.3f, 0.78f, 0.5f, 0.78f)
        cubicTo(0.7f, 0.78f, 0.9f, 0.8f, 1f, 0.9f)
        lineTo(1.02f, 1.02f); close()
    }
    // neck first so the collar overlaps it
    drawRect(style.skin, Offset(0.43f, 0.64f), Size(0.14f, 0.18f))
    drawRect(Color.Black.copy(alpha = SkinShadeAlpha), Offset(0.43f, 0.64f), Size(0.14f, 0.05f))
    when (style.outfit) {
        Outfit.Sailor -> {
            drawPath(shoulders, Navy)
            drawPath(path {
                moveTo(0.3f, 0.8f); lineTo(0.5f, 0.97f); lineTo(0.7f, 0.8f)
                lineTo(0.84f, 0.9f); lineTo(0.66f, 1.02f); lineTo(0.5f, 0.93f); lineTo(0.34f, 1.02f); lineTo(0.16f, 0.9f); close()
            }, Navy)
            drawPath(path { moveTo(0.3f, 0.8f); lineTo(0.5f, 0.97f); lineTo(0.7f, 0.8f) }, Paper, style = Stroke(0.018f, cap = StrokeCap.Round))
            drawPath(path { moveTo(0.5f, 0.95f); lineTo(0.43f, 1.02f); lineTo(0.57f, 1.02f); close() }, Pink)
            drawCircle(Pink, 0.022f, Offset(0.5f, 0.945f))
        }
        Outfit.Gakuran -> {
            drawPath(shoulders, Color(0xFF0E0E14))
            drawRect(Color(0xFF0E0E14), Offset(0.4f, 0.74f), Size(0.2f, 0.1f))
            drawPath(path { moveTo(0.43f, 0.8f); lineTo(0.5f, 0.9f); lineTo(0.57f, 0.8f) }, style.skin)
            drawCircle(Color(0xFFD9B44A), 0.016f, Offset(0.5f, 0.94f))
            drawCircle(Color(0xFFD9B44A), 0.016f, Offset(0.5f, 1.0f))
        }
    }
}

private fun DrawScope.drawFace(style: AvatarStyle) {
    val face = path {
        moveTo(0.25f, 0.36f)
        cubicTo(0.25f, 0.62f, 0.36f, 0.8f, 0.5f, 0.83f)
        cubicTo(0.64f, 0.8f, 0.75f, 0.62f, 0.75f, 0.36f)
        cubicTo(0.75f, 0.2f, 0.25f, 0.2f, 0.25f, 0.36f)
        close()
    }
    drawPath(face, style.skin)
    drawOval(Pink.copy(alpha = 0.25f), Offset(0.27f, 0.64f), Size(0.1f, 0.045f))
    drawOval(Pink.copy(alpha = 0.25f), Offset(0.63f, 0.64f), Size(0.1f, 0.045f))
}

private fun DrawScope.drawFrontHair(style: AvatarStyle) {
    val hair = style.hair
    val dark = lerp(hair, Color.Black, 0.3f)
    // side locks framing the face
    if (style.hairStyle == HairStyle.Long || style.hairStyle == HairStyle.TwinTails) {
        drawPath(path {
            moveTo(0.2f, 0.44f); cubicTo(0.17f, 0.6f, 0.19f, 0.76f, 0.22f, 0.88f)
            lineTo(0.28f, 0.8f); cubicTo(0.27f, 0.68f, 0.27f, 0.56f, 0.28f, 0.44f); close()
        }, hair)
        drawPath(path {
            moveTo(0.8f, 0.44f); cubicTo(0.83f, 0.6f, 0.81f, 0.76f, 0.78f, 0.88f)
            lineTo(0.72f, 0.8f); cubicTo(0.73f, 0.68f, 0.73f, 0.56f, 0.72f, 0.44f); close()
        }, hair)
    } else {
        drawPath(path { moveTo(0.2f, 0.44f); lineTo(0.23f, 0.62f); lineTo(0.28f, 0.48f); close() }, hair)
        drawPath(path { moveTo(0.8f, 0.44f); lineTo(0.77f, 0.62f); lineTo(0.72f, 0.48f); close() }, hair)
    }
    if (style.hairStyle == HairStyle.Spiky) {
        drawPath(path {
            moveTo(0.2f, 0.3f); lineTo(0.22f, 0.04f); lineTo(0.36f, 0.16f); lineTo(0.44f, -0.04f)
            lineTo(0.54f, 0.14f); lineTo(0.68f, -0.02f); lineTo(0.72f, 0.16f); lineTo(0.84f, 0.06f); lineTo(0.8f, 0.34f); close()
        }, dark)
    }
    // bangs: solid underlayer so no skin shows between strands, then strand tips
    drawPath(path {
        moveTo(0.2f, 0.5f); cubicTo(0.08f, 0.0f, 0.92f, 0.0f, 0.8f, 0.5f)
        lineTo(0.74f, 0.4f); lineTo(0.26f, 0.4f); close()
    }, hair)
    drawPath(path {
        moveTo(0.19f, 0.5f)
        cubicTo(0.1f, 0.0f, 0.9f, 0.0f, 0.81f, 0.5f)
        lineTo(0.75f, 0.33f); lineTo(0.71f, 0.47f); lineTo(0.64f, 0.3f); lineTo(0.58f, 0.45f)
        lineTo(0.5f, 0.3f); lineTo(0.42f, 0.45f); lineTo(0.36f, 0.3f); lineTo(0.29f, 0.47f); lineTo(0.25f, 0.33f)
        close()
    }, hair)
    drawPath(path { moveTo(0.3f, 0.28f); quadraticTo(0.5f, 0.15f, 0.7f, 0.28f) }, Color.White.copy(alpha = 0.22f), style = Stroke(0.03f, cap = StrokeCap.Round))
}

private fun DrawScope.drawEye(style: AvatarStyle, cx: Float) {
    val cy = 0.575f
    val outer = if (cx < 0.5f) -1f else 1f
    val halfW = 0.088f
    val eye = path {
        moveTo(cx - halfW, cy)
        quadraticTo(cx, cy - 0.12f, cx + halfW, cy - 0.015f)
        quadraticTo(cx, cy + 0.1f, cx - halfW, cy)
        close()
    }
    drawPath(eye, Color.White)
    clipPath(eye) {
        drawOval(
            Brush.verticalGradient(listOf(lerp(style.eyes, Color.Black, 0.55f), style.eyes, lerp(style.eyes, Color.White, 0.45f)), cy - 0.09f, cy + 0.09f),
            Offset(cx - 0.056f, cy - 0.095f), Size(0.112f, 0.19f),
        )
        drawOval(Ink, Offset(cx - 0.028f, cy - 0.05f), Size(0.056f, 0.11f))
        drawCircle(Color.White, 0.026f, Offset(cx - 0.028f, cy - 0.04f))
        drawCircle(Color.White, 0.012f, Offset(cx + 0.03f, cy + 0.045f))
    }
    // thick upper lash line with outward flick
    drawPath(
        path {
            moveTo(cx - halfW - 0.005f, cy + 0.004f)
            quadraticTo(cx, cy - 0.13f, cx + halfW + 0.005f, cy - 0.018f)
            lineTo(cx + outer * (halfW + 0.025f), cy - 0.045f)
        },
        Ink, style = Stroke(0.024f, cap = StrokeCap.Round),
    )
}

private fun DrawScope.drawNoseAndMouth() {
    drawPath(path { moveTo(0.5f, 0.655f); lineTo(0.494f, 0.675f); lineTo(0.508f, 0.675f) }, Color(0xFFD99A83), style = Stroke(0.007f, cap = StrokeCap.Round))
    drawPath(
        path { moveTo(0.46f, 0.725f); quadraticTo(0.5f, 0.755f, 0.54f, 0.725f) },
        Color(0xFFB5524F), style = Stroke(0.011f, cap = StrokeCap.Round),
    )
}
