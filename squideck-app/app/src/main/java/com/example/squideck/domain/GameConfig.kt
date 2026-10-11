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
) {
    init {
        require(playerCount * handSize == setCount * copiesPerSet) { "Regular cards must be dealt exactly" }
    }
}
