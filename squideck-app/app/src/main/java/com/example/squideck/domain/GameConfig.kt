package com.example.squideck.domain

/** Tunable game constants. Defaults are the confirmed rules in FEATURES.md. */
data class GameConfig(
    val playerCount: Int = 5,
    val handSize: Int = 4,
    val setCount: Int = 4,
    val copiesPerSet: Int = 5,
    val winSetSize: Int = 4,
    val winnerSlots: Int = 3,
) {
    init {
        require(playerCount * handSize == setCount * copiesPerSet) { "Regular cards must be dealt exactly" }
    }
}
