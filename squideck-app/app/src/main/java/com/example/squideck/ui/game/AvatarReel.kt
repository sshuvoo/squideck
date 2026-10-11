package com.example.squideck.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlin.math.roundToInt
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.squideck.ui.theme.MaskWhite
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition


private const val REEL_MS = 1100

/**
 * Slot-machine reveal: faces whip upward past the window, slow down and settle on [style].
 * [onSettled] fires when the reel stops so the caller can fade the player's name in.
 */
@Composable
fun AvatarReel(
    style: AvatarStyle,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    onSettled: () -> Unit = {},
) {
    val frames = remember(style) { List(3) { AvatarStyles.all.shuffled() }.flatten() + style }
    val progress = remember { Animatable(0f) }
    val pop = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(REEL_MS, easing = CubicBezierEasing(0.2f, 0.7f, 0.1f, 1f)))
        onSettled()
        pop.animateTo(1.12f, tween(90))
        pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
    }
    // Only the two faces crossing the window are drawn: the one leaving at the top, the next one rising in.
    val position = progress.value * (frames.size - 1)
    val current = position.toInt().coerceIn(0, frames.size - 1)
    val next = (current + 1).coerceAtMost(frames.size - 1)
    val fraction = position - current
    Box(
        modifier
            .size(size)
            .scale(pop.value)
            .clip(CircleShape)
            .border(1.5.dp, MaskWhite.copy(alpha = 0.3f), CircleShape),
    ) {
        AnimeAvatar(frames[current], Modifier.size(size).offset { IntOffset(0, (-fraction * size.toPx()).roundToInt()) })
        if (next != current) {
            AnimeAvatar(frames[next], Modifier.size(size).offset { IntOffset(0, ((1f - fraction) * size.toPx()).roundToInt()) })
        }
    }
}

/** A seat nobody has taken yet: a pulsing dashed circle. */
@Composable
fun EmptyAvatarSlot(modifier: Modifier = Modifier, size: Dp = 56.dp) {
    val pulse by rememberInfiniteTransition(label = "searching").animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse",
    )
    Canvas(modifier.size(size).alpha(pulse)) {
        drawCircle(
            Color.White.copy(alpha = 0.6f),
            style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))),
            radius = this.size.minDimension / 2 - 2.dp.toPx(),
        )
    }
}
