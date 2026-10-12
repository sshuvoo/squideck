package com.example.squideck.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.squideck.domain.Card
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Releasing this far above the hand (as a share of the card's height) passes the card. */
private const val THROW_DISTANCE = 0.45f

/** An upward flick this fast (px/s) passes the card even if it hasn't travelled far. */
private const val FLICK_VELOCITY = 1_000f

/**
 * A card in the local player's hand that is passed by dragging it up and letting go, or by flicking it
 * upward. Tapping a passable card makes it hop to teach the gesture; tapping a blocked one shakes it.
 * Screen-reader users get a "pass" custom action instead of the gesture.
 */
@Composable
fun ThrowableCard(
    card: Card,
    received: Boolean,
    blocked: Boolean,
    enabled: Boolean,
    throwable: Boolean,
    width: Dp,
    passLabel: String,
    onThrow: () -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val offset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var dragging by remember { mutableStateOf(false) }
    val throwDistancePx = with(LocalDensity.current) { (width * (CardBaseHeight / CardBaseWidth)).toPx() * THROW_DISTANCE }
    val hopPx = with(LocalDensity.current) { 18.dp.toPx() }
    val armed = dragging && offset.value.y < -throwDistancePx

    fun settle() {
        scope.launch { offset.animateTo(Offset.Zero, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) }
    }

    fun pass() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onThrow()
        // If the move was not accepted (for example the turn ran out at the same moment), come back to the hand.
        scope.launch {
            delay(400)
            offset.snapTo(Offset.Zero)
        }
    }

    CardView(
        card = card,
        selected = armed,
        received = received,
        blocked = blocked,
        enabled = enabled,
        width = width,
        onClick = {
            onTap()
            scope.launch {
                if (throwable) {
                    offset.animateTo(Offset(0f, -hopPx), tween(120))
                    offset.animateTo(Offset.Zero, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                } else if (blocked) {
                    listOf(10f, -10f, 6f, -6f, 0f).forEach { offset.animateTo(Offset(it, 0f), tween(45)) }
                }
            }
        },
        modifier = Modifier
            .zIndex(if (dragging) 1f else 0f)
            // Gestures read finger movement in the card's resting place, so they come before the offset.
            .then(
                if (throwable) {
                    Modifier
                        .pointerInput(card.id) {
                            // Tracked synchronously: the animated offset catches up a frame later, which is too
                            // late to judge a fast flick when the finger lifts.
                            var dragged = Offset.Zero
                            val flick = FlickMeter()
                            detectDragGestures(
                                onDragStart = {
                                    dragging = true
                                    dragged = offset.value
                                    flick.reset()
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragged += amount
                                    flick.add(change.uptimeMillis, dragged.y)
                                    val target = dragged
                                    scope.launch { offset.snapTo(target) }
                                },
                                onDragEnd = {
                                    dragging = false
                                    val flickedUp = flick.velocityY() < -FLICK_VELOCITY && dragged.y < 0f
                                    if (dragged.y < -throwDistancePx || flickedUp) pass() else settle()
                                },
                                onDragCancel = {
                                    dragging = false
                                    settle()
                                },
                            )
                        }
                        .semantics { customActions = listOf(CustomAccessibilityAction(passLabel) { pass(); true }) }
                } else Modifier,
            )
            .offset { IntOffset(offset.value.x.roundToInt(), offset.value.y.roundToInt()) }
            .graphicsLayer {
                val lift = if (dragging) 1.08f else 1f
                scaleX = lift
                scaleY = lift
                rotationZ = offset.value.x / 18f
            }
            // The caller's modifier reports the card's on-screen position, so it must see the drag offset.
            .then(modifier),
    )
}

/**
 * Vertical speed of the drag over its last [WINDOW_MS], in px/s. Measured from the drag samples
 * themselves so a quick flick counts even when the finger lifts mid-motion.
 */
private class FlickMeter {
    private val samples = ArrayDeque<Pair<Long, Float>>()

    fun reset() = samples.clear()

    fun add(timeMs: Long, y: Float) {
        samples.addLast(timeMs to y)
        while (samples.size > 2 && timeMs - samples.first().first > WINDOW_MS) samples.removeFirst()
    }

    fun velocityY(): Float {
        if (samples.size < 2) return 0f
        val (t0, y0) = samples.first()
        val (t1, y1) = samples.last()
        return if (t1 > t0) (y1 - y0) / (t1 - t0) * 1000f else 0f
    }

    private companion object {
        const val WINDOW_MS = 100L
    }
}
