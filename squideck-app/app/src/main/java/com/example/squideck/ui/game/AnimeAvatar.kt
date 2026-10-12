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
import com.example.squideck.ui.theme.Crimson
import com.example.squideck.ui.theme.DarkTeal
import com.example.squideck.ui.theme.Ink
import com.example.squideck.ui.theme.Pink
import com.example.squideck.ui.theme.Teal
import com.example.squideck.ui.theme.White
import com.example.squideck.ui.theme.shade
import com.example.squideck.ui.theme.tint

enum class HairStyle { Long, Short, Spiky, TwinTails, Hooded }
enum class Outfit { Sailor, Gakuran, Jumpsuit }

data class AvatarStyle(
    val hairStyle: HairStyle,
    val outfit: Outfit,
    val hair: Color,
    val eyes: Color,
    val background: Color,
    // The brand palette has no skin tone, so faces are white with ink linework, manga style.
    val skin: Color = White,
)

/**
 * Every face in the game, addressed by avatar id (the index). There is one per seat, so when the player picks
 * theirs the bots take the other four and nobody at the table looks alike.
 */
object AvatarStyles {
    val all = listOf(
        AvatarStyle(HairStyle.Long, Outfit.Sailor, hair = Ink, eyes = Pink, background = Teal),
        AvatarStyle(HairStyle.Spiky, Outfit.Gakuran, hair = Crimson, eyes = Teal, background = DarkTeal),
        AvatarStyle(HairStyle.TwinTails, Outfit.Sailor, hair = Pink, eyes = DarkTeal, background = Ink),
        AvatarStyle(HairStyle.Short, Outfit.Gakuran, hair = DarkTeal, eyes = Crimson, background = Crimson),
        AvatarStyle(HairStyle.Hooded, Outfit.Jumpsuit, hair = Ink, eyes = Teal, background = DarkTeal),
    )

    /** New players start as the pink-hooded contestant. */
    const val DEFAULT_PLAYER_ID = 4

    fun byId(id: Int): AvatarStyle = all[id.mod(all.size)]

    /** The faces left for the bots once the player has chosen theirs. */
    fun idsExcept(playerId: Int): List<Int> = all.indices.filter { it != playerId.mod(all.size) }
}

/** Sailor uniforms are dark teal; gakuran jackets are ink, like the guards' masks. */
private val SailorColor = DarkTeal
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
                Brush.verticalGradient(listOf(style.background, style.background.shade(0.35f)), 0f, 1f),
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
    val dark = style.hair.shade(0.3f)
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
            drawCircle(Teal, 0.035f, Offset(0.19f, 0.33f))
            drawCircle(Teal, 0.035f, Offset(0.81f, 0.33f))
        }
        // The guards' pink hood, with a darker lining behind the face.
        HairStyle.Hooded -> {
            drawPath(path {
                moveTo(0.06f, 0.6f); cubicTo(0.0f, -0.18f, 1.0f, -0.18f, 0.94f, 0.6f)
                lineTo(0.96f, 1.0f); lineTo(0.04f, 1.0f); close()
            }, Pink)
            drawPath(path {
                moveTo(0.17f, 0.55f); cubicTo(0.14f, 0.1f, 0.86f, 0.1f, 0.83f, 0.55f)
                lineTo(0.82f, 0.86f); lineTo(0.18f, 0.86f); close()
            }, Pink.shade(0.45f))
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
    drawRect(Ink.copy(alpha = SkinShadeAlpha), Offset(0.43f, 0.64f), Size(0.14f, 0.05f))
    when (style.outfit) {
        Outfit.Sailor -> {
            drawPath(shoulders, SailorColor)
            drawPath(path {
                moveTo(0.3f, 0.8f); lineTo(0.5f, 0.97f); lineTo(0.7f, 0.8f)
                lineTo(0.84f, 0.9f); lineTo(0.66f, 1.02f); lineTo(0.5f, 0.93f); lineTo(0.34f, 1.02f); lineTo(0.16f, 0.9f); close()
            }, SailorColor)
            drawPath(path { moveTo(0.3f, 0.8f); lineTo(0.5f, 0.97f); lineTo(0.7f, 0.8f) }, White, style = Stroke(0.018f, cap = StrokeCap.Round))
            drawPath(path { moveTo(0.5f, 0.95f); lineTo(0.43f, 1.02f); lineTo(0.57f, 1.02f); close() }, Pink)
            drawCircle(Pink, 0.022f, Offset(0.5f, 0.945f))
        }
        Outfit.Gakuran -> {
            drawPath(shoulders, Ink)
            drawRect(Ink, Offset(0.4f, 0.74f), Size(0.2f, 0.1f))
            drawPath(path { moveTo(0.43f, 0.8f); lineTo(0.5f, 0.9f); lineTo(0.57f, 0.8f) }, style.skin)
            drawCircle(White, 0.016f, Offset(0.5f, 0.94f))
            drawCircle(White, 0.016f, Offset(0.5f, 1.0f))
        }
        // The contestants' pink jumpsuit with its black zip.
        Outfit.Jumpsuit -> {
            drawPath(shoulders, Pink)
            drawPath(path { moveTo(0.5f, 0.8f); lineTo(0.5f, 1.02f) }, Ink, style = Stroke(0.02f))
            drawPath(path { moveTo(0.4f, 0.8f); lineTo(0.5f, 0.86f); lineTo(0.6f, 0.8f) }, Pink.shade(0.3f), style = Stroke(0.02f, cap = StrokeCap.Round))
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
    val dark = hair.shade(0.3f)
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
    if (style.hairStyle == HairStyle.Hooded) {
        // Hood rim over the top of the head, framing the fringe.
        drawPath(
            path { moveTo(0.15f, 0.62f); cubicTo(0.1f, 0.03f, 0.9f, 0.03f, 0.85f, 0.62f) },
            Pink, style = Stroke(0.07f, cap = StrokeCap.Round),
        )
    } else {
        drawPath(path { moveTo(0.3f, 0.28f); quadraticTo(0.5f, 0.15f, 0.7f, 0.28f) }, White.copy(alpha = 0.22f), style = Stroke(0.03f, cap = StrokeCap.Round))
    }
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
    drawPath(eye, White)
    clipPath(eye) {
        drawOval(
            Brush.verticalGradient(listOf(Ink, style.eyes, style.eyes.tint(0.35f)), cy - 0.09f, cy + 0.09f),
            Offset(cx - 0.056f, cy - 0.095f), Size(0.112f, 0.19f),
        )
        drawOval(Ink, Offset(cx - 0.028f, cy - 0.05f), Size(0.056f, 0.11f))
        drawCircle(White, 0.026f, Offset(cx - 0.028f, cy - 0.04f))
        drawCircle(White, 0.012f, Offset(cx + 0.03f, cy + 0.045f))
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
    drawPath(path { moveTo(0.5f, 0.655f); lineTo(0.494f, 0.675f); lineTo(0.508f, 0.675f) }, Crimson.copy(alpha = 0.45f), style = Stroke(0.007f, cap = StrokeCap.Round))
    drawPath(
        path { moveTo(0.46f, 0.725f); quadraticTo(0.5f, 0.755f, 0.54f, 0.725f) },
        Crimson, style = Stroke(0.011f, cap = StrokeCap.Round),
    )
}
