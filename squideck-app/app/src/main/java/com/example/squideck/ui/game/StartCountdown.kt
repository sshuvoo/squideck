package com.example.squideck.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.squideck.domain.TurnTimer
import com.example.squideck.ui.components.GameText
import com.example.squideck.ui.theme.Ink
import com.example.squideck.ui.theme.Pink
import kotlin.math.pow

/**
 * Get-ready overlay before a round's first turn: a big 3, 2, 1, then "START!", each popping in, with a haptic
 * tick per step. The last [flashMs] of [timer] is the START flash. It also blocks touches so nobody moves early.
 */
@Composable
fun StartCountdown(timer: TurnTimer, flashMs: Long, modifier: Modifier = Modifier) {
    val remaining = rememberRemainingMs(timer)
    // Time left in the 3-2-1 part; at or below zero the START flash is showing.
    val countdownLeft by remember(timer) { derivedStateOf { remaining.value - flashMs } }
    val label by remember(timer) { derivedStateOf { if (countdownLeft > 0) "${secondsLeft(countdownLeft)}" else "Start!" } }
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(label) { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
    Box(
        modifier
            .fillMaxSize()
            .background(Ink.copy(alpha = 0.72f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val starting = label == "Start!"
            GameText("Get ready", Modifier.graphicsLayer { alpha = if (starting) 0f else 1f }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
            GameText(
                label,
                Modifier.graphicsLayer {
                    // Each step pops in large and settles; numbers fade out just before the next one arrives.
                    val left = countdownLeft
                    val t = if (left > 0) 1f - ((left % 1000L) / 1000f).let { if (it == 0f) 1f else it } else (-left).toFloat() / flashMs
                    val pop = 1f + 0.6f * (1f - t.coerceIn(0f, 1f) * if (left > 0) 1f else 3f).coerceAtLeast(0f).pow(4)
                    scaleX = pop
                    scaleY = pop
                    alpha = if (left > 0 && t > 0.8f) (1f - t) / 0.2f else 1f
                },
                color = Pink,
                fontSize = if (starting) 72.sp else 120.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}
