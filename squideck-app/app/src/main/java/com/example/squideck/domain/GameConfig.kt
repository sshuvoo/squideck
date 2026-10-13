package com.example.squideck.domain

/** Tunable game constants. Defaults are the confirmed rules in FEATURES.md. */
data class GameConfig(
    val playerCount: Int = 5,
    val handSize: Int = 4,
    val setCount: Int = 4,
    val copiesPerSet: Int = 5,
    val winSetSize: Int = 4,
    /** The round ends once this many players have won; everyone else lost. */
    val winnerSlots: Int = 3,
    /** Time each player gets to move before a bot plays for them. */
    val turnTimeLimitMs: Long = 15_000,
    /** Get-ready countdown (3, 2, 1) shown to everyone on the board before the first turn of a round. */
    val startCountdownMs: Long = 3_000,
    /** How long "START!" stays up after the countdown, before the first turn begins. */
    val startFlashMs: Long = 700,
) {
    init {
        require(playerCount * handSize == setCount * copiesPerSet) { "Regular cards must be dealt exactly" }
    }
}
