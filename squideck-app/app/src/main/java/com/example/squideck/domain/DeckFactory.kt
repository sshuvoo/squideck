package com.example.squideck.domain

import kotlin.random.Random

data class Deck(val regular: List<Card>, val imposter: Card)

/** Builds a freshly shuffled deck: [GameConfig.setCount] random symbols, each with equal copies, plus the Imposter. */
class DeckFactory(private val config: GameConfig) {
    fun create(random: Random): Deck {
        val symbols = Symbol.entries.shuffled(random).take(config.setCount)
        val regular = symbols
            .flatMap { symbol -> List(config.copiesPerSet) { symbol } }
            .mapIndexed { id, symbol -> Card(id, symbol) }
            .shuffled(random)
        return Deck(regular, imposter = Card(regular.size, symbol = null))
    }
}
