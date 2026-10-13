package com.example.squideck.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.squideck.ui.theme.Crimson
import com.example.squideck.ui.theme.DarkTeal
import com.example.squideck.ui.theme.GameFont
import com.example.squideck.ui.theme.Ink
import com.example.squideck.ui.theme.Pink
import com.example.squideck.ui.theme.Teal
import com.example.squideck.ui.theme.White
import com.example.squideck.ui.theme.shade
import com.example.squideck.ui.theme.tint
import kotlin.math.abs
import kotlin.math.roundToInt

enum class GameButtonStyle { Primary, Secondary, Ghost }

/** Face height of every key; with its 3D base every button (and icon button) stands [GameButtonHeight] tall. */
private val KeyFaceHeight = 36.dp
private val KeyDepth = 4.dp
val GameButtonHeight = KeyFaceHeight + KeyDepth

/** Sizes share one height so buttons line up anywhere; they differ only in lettering and padding. */
enum class GameButtonSize(val fontSize: Int, val horizontalPadding: Dp) {
    Large(fontSize = 14, horizontalPadding = 20.dp),
    Small(fontSize = 12, horizontalPadding = 14.dp),
    ;

    val height: Dp get() = KeyFaceHeight
    val depth: Dp get() = KeyDepth
    val radius: Dp get() = 12.dp
}

/** One shine cycle: the sweep crosses during the first part, then the key rests. */
private const val SHINE_CYCLE_MS = 3_200
private const val SHINE_SHARE = 0.38f

/** Colours of one key, all taken from the brand palette. */
private class KeyLook(
    val faceTop: Color,
    val faceMid: Color,
    val faceBottom: Color,
    val wallTop: Color,
    val wallBottom: Color,
    val content: Color,
    /** Colour of the label's soft drop shadow, a deep version of the key colour. */
    val textShadow: Color?,
    val glow: Color?,
    val shine: Boolean,
)

private fun lookFor(style: GameButtonStyle, enabled: Boolean): KeyLook = when {
    !enabled -> KeyLook(
        faceTop = Ink.tint(0.16f), faceMid = Ink.tint(0.12f), faceBottom = Ink.tint(0.08f),
        wallTop = Ink.tint(0.08f), wallBottom = Ink.tint(0.04f),
        content = White.copy(alpha = 0.4f), textShadow = null, glow = null, shine = false,
    )
    style == GameButtonStyle.Primary -> KeyLook(
        faceTop = Pink.tint(0.2f), faceMid = Pink, faceBottom = Crimson,
        wallTop = Crimson.shade(0.2f), wallBottom = Crimson.shade(0.32f),
        content = White, textShadow = Crimson.shade(0.35f), glow = Pink, shine = true,
    )
    else -> KeyLook(
        faceTop = Teal.tint(0.16f), faceMid = Teal, faceBottom = DarkTeal,
        wallTop = DarkTeal.shade(0.2f), wallBottom = DarkTeal.shade(0.34f),
        content = White, textShadow = DarkTeal.shade(0.45f), glow = null, shine = false,
    )
}

/**
 * The game's one button: a glossy 3D key. A rounded face with a convex gradient sits on an extruded base,
 * with a bevel light along the top, a soft inner shadow at the bottom, a glass dome highlight and rounded
 * game lettering. The main action also breathes a pink glow and gets a shine sweep with a sparkle. Pressing
 * sinks the face into its base with a bounce and a haptic tick. Ghost buttons are text only.
 */
@Composable
fun GameButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: GameButtonStyle = GameButtonStyle.Primary,
    size: GameButtonSize = GameButtonSize.Large,
    enabled: Boolean = true,
    leading: String? = null,
) {
    val ghost = style == GameButtonStyle.Ghost
    val look = lookFor(style, enabled)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    val sink by animateFloatAsState(
        if (pressed && enabled) 1f else 0f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "sink",
    )
    val motion = rememberKeyMotion(animate = enabled && look.shine)
    val depth = size.depth
    val outer = RoundedCornerShape(size.radius)

    BoxWithConstraints(
        modifier
            .height(size.height + depth)
            .then(
                when {
                    ghost -> Modifier
                    look.glow != null -> Modifier.shadow((8f + 6f * motion.breath.value).dp, outer, clip = false, ambientColor = look.glow, spotColor = look.glow)
                    else -> Modifier.shadow((8f - 5f * sink).dp, outer, clip = false, ambientColor = Ink, spotColor = Ink)
                },
            )
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
    ) {
        // Wraps its label unless the caller fixes the width (fillMaxWidth, weight, width).
        val fixedWidth = constraints.hasBoundedWidth && constraints.minWidth == constraints.maxWidth
        Box(
            Modifier
                .then(if (fixedWidth) Modifier.fillMaxWidth() else Modifier)
                .height(size.height + depth)
                .then(if (ghost) Modifier else Modifier.drawBehind { drawKey(look, size, motion, sink, pressed && enabled) }),
        ) {
            Box(
                Modifier
                    .then(if (fixedWidth) Modifier.fillMaxWidth() else Modifier)
                    // Text-only buttons have no base, so their label centres on the full button height.
                    .height(if (ghost) size.height + depth else size.height)
                    .offset { IntOffset(0, (depth.toPx() * sink).roundToInt()) }
                    .padding(horizontal = size.horizontalPadding),
                contentAlignment = Alignment.Center,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (leading != null) KeyLabel(leading, size, look, ghost)
                    KeyLabel(text.uppercase(), size, look, ghost)
                }
            }
        }
    }
}

/** Rounded game lettering with a soft drop shadow in a deep version of the key colour. */
@Composable
private fun KeyLabel(text: String, size: GameButtonSize, look: KeyLook, ghost: Boolean) {
    GameText(
        text,
        maxLines = 1,
        style = TextStyle(
            fontFamily = GameFont,
            fontSize = size.fontSize.sp,
            fontWeight = if (ghost) FontWeight.SemiBold else FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = if (ghost) White.copy(alpha = 0.78f) else look.content,
            shadow = look.textShadow?.takeIf { !ghost }?.let { Shadow(it, Offset(0f, 2.5f), 1f) },
        ),
    )
}

private class KeyMotion(val shine: State<Float>, val breath: State<Float>)

private val Still = object : State<Float> { override val value = 0f }
private val StillMotion = KeyMotion(Still, Still)

/** Only the live main action animates; every other key stays still. */
@Composable
private fun rememberKeyMotion(animate: Boolean): KeyMotion {
    if (!animate) return StillMotion
    val transition = rememberInfiniteTransition(label = "key")
    val shine = transition.animateFloat(0f, 1f, infiniteRepeatable(tween(SHINE_CYCLE_MS, easing = LinearEasing)), label = "shine")
    val breath = transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1_400), RepeatMode.Reverse), label = "breath")
    return remember(shine, breath) { KeyMotion(shine, breath) }
}

private fun DrawScope.drawKey(look: KeyLook, keySize: GameButtonSize, motion: KeyMotion, sink: Float, pressed: Boolean) {
    val w = size.width
    val faceH = keySize.height.toPx()
    val depth = keySize.depth.toPx()
    val travel = depth * sink
    val r = keySize.radius.toPx()

    // Extruded base: the key's side wall, seen below the face.
    drawRoundRect(
        Brush.verticalGradient(listOf(look.wallTop, look.wallBottom), startY = depth, endY = depth + faceH),
        topLeft = Offset(0f, depth), size = Size(w, faceH), cornerRadius = CornerRadius(r),
    )

    // Face: convex gradient, lighter at the top.
    val faceTop = travel
    val face = RoundRect(0f, faceTop, w, faceTop + faceH, CornerRadius(r))
    val facePath = Path().apply { addRoundRect(face) }
    drawPath(
        facePath,
        Brush.verticalGradient(
            0f to look.faceTop, 0.45f to look.faceMid, 1f to look.faceBottom,
            startY = faceTop, endY = faceTop + faceH,
        ),
    )

    clipPath(facePath) {
        // Glass dome highlight across the upper half.
        val inset = 5.dp.toPx()
        drawRoundRect(
            Brush.verticalGradient(listOf(White.copy(alpha = if (pressed) 0.5f else 0.38f), White.copy(alpha = 0.04f)), startY = faceTop + inset, endY = faceTop + faceH * 0.5f),
            topLeft = Offset(inset * 1.6f, faceTop + inset * 0.8f),
            size = Size(w - inset * 3.2f, faceH * 0.44f),
            cornerRadius = CornerRadius(r * 0.75f),
        )
        // Soft inner shadow along the bottom edge for roundness.
        drawRect(
            Brush.verticalGradient(listOf(Ink.copy(alpha = 0f), Ink.copy(alpha = 0.08f)), startY = faceTop + faceH * 0.62f, endY = faceTop + faceH),
            topLeft = Offset(0f, faceTop + faceH * 0.62f), size = Size(w, faceH * 0.38f),
        )
        // Diagonal shine sweep on the main action.
        if (look.shine) {
            val t = motion.shine.value
            if (t < SHINE_SHARE) {
                val x = -w * 0.25f + (w * 1.5f) * (t / SHINE_SHARE)
                drawRect(
                    Brush.linearGradient(
                        listOf(White.copy(alpha = 0f), White.copy(alpha = 0.5f), White.copy(alpha = 0f)),
                        start = Offset(x - faceH * 0.9f, faceTop),
                        end = Offset(x, faceTop + faceH),
                    ),
                )
            }
        }
    }

    // Bevel: a bright line along the top inner edge, fading out by the middle.
    val bevelInset = 2.dp.toPx()
    drawRoundRect(
        Brush.verticalGradient(listOf(White.copy(alpha = 0.7f), White.copy(alpha = 0f)), startY = faceTop, endY = faceTop + faceH * 0.55f),
        topLeft = Offset(bevelInset, faceTop + bevelInset),
        size = Size(w - bevelInset * 2, faceH - bevelInset * 2),
        cornerRadius = CornerRadius(r - bevelInset),
        style = Stroke(1.5.dp.toPx()),
    )

    // A sparkle near the top-left corner that flashes as the shine sweeps past it.
    if (look.shine) {
        val t = motion.shine.value / SHINE_SHARE
        val glint = (1f - abs(t - 0.22f) / 0.12f).coerceIn(0f, 1f)
        if (glint > 0f) drawSparkle(Offset(r * 1.2f, faceTop + faceH * 0.3f), 7.dp.toPx() * glint, glint)
    }
}

/** A four-point star glint. */
private fun DrawScope.drawSparkle(center: Offset, radius: Float, alpha: Float) {
    val thin = radius * 0.22f
    val star = Path().apply {
        moveTo(center.x, center.y - radius)
        lineTo(center.x + thin, center.y - thin)
        lineTo(center.x + radius, center.y)
        lineTo(center.x + thin, center.y + thin)
        lineTo(center.x, center.y + radius)
        lineTo(center.x - thin, center.y + thin)
        lineTo(center.x - radius, center.y)
        lineTo(center.x - thin, center.y - thin)
        close()
    }
    drawPath(star, White.copy(alpha = alpha))
}
