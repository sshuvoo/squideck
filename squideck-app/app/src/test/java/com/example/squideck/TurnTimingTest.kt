package com.example.squideck

import com.example.squideck.domain.RandomThinkTime
import com.example.squideck.domain.TurnTimer
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TurnTimingTest {
    @Test
    fun randomThinkTimeStaysInRangeAndVaries() {
        val think = RandomThinkTime(1_000L..3_000L, Random(7))
        val samples = List(200) { think.nextMs() }
        assertTrue(samples.all { it in 1_000L..3_000L })
        assertTrue(samples.toSet().size > 50)
    }

    @Test(expected = IllegalArgumentException::class)
    fun randomThinkTimeRejectsEmptyRange() {
        RandomThinkTime(5L..1L)
    }

    @Test
    fun turnTimerCountsDownAndClamps() {
        val timer = TurnTimer(seat = 2, startedAtMs = 1_000, durationMs = 15_000)
        assertEquals(15_000, timer.remainingMs(1_000))
        assertEquals(10_000, timer.remainingMs(6_000))
        assertEquals(0, timer.remainingMs(99_000))
        assertEquals(15_000, timer.remainingMs(0))
    }
}
