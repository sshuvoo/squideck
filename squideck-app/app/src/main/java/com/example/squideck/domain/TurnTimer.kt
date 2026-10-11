package com.example.squideck.domain

/** Monotonic millisecond clock shared by the session (which starts timers) and the UI (which draws them). */
object MonotonicClock {
    fun nowMs(): Long = System.nanoTime() / 1_000_000
}

/** The countdown for the player currently on turn. */
data class TurnTimer(val seat: Int, val startedAtMs: Long, val durationMs: Long) {
    fun remainingMs(nowMs: Long = MonotonicClock.nowMs()): Long =
        (durationMs - (nowMs - startedAtMs)).coerceIn(0, durationMs)
}
