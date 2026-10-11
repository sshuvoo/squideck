package com.example.squideck.domain

import kotlin.random.Random

/** How long a bot "thinks" before moving. */
fun interface ThinkTime {
    fun nextMs(): Long
}

/** Uniformly random think time, so every bot (and every turn) moves at a different moment. */
class RandomThinkTime(
    private val range: LongRange = 1_500L..6_000L,
    private val random: Random = Random.Default,
) : ThinkTime {
    init {
        require(!range.isEmpty() && range.first >= 0) { "Think time range must be non-empty and non-negative" }
    }

    override fun nextMs(): Long = random.nextLong(range.first, range.last + 1)
}
