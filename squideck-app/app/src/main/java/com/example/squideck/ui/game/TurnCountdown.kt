package com.example.squideck.ui.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.withFrameMillis
import com.example.squideck.domain.TurnTimer
import com.example.squideck.ui.theme.Amber
import com.example.squideck.ui.theme.Pink

/** Below this the countdown switches to the warning colour. */
const val URGENT_MS = 5_000L

fun countdownColor(remainingMs: Long): Color = if (remainingMs <= URGENT_MS) Amber else Pink

fun secondsLeft(remainingMs: Long): Int = ((remainingMs + 999) / 1000).toInt()

/**
 * Remaining time of [timer], updated every frame. Read it only inside draw lambdas for smooth
 * animation without recomposition; derive whole seconds with `derivedStateOf` for text.
 */
@Composable
fun rememberRemainingMs(timer: TurnTimer?): State<Long> =
    produceState(timer?.durationMs ?: 0L, timer) {
        if (timer == null) return@produceState
        while (true) {
            value = timer.remainingMs()
            if (value <= 0L) break
            withFrameMillis { }
        }
    }
