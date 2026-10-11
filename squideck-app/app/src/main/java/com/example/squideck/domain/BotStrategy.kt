package com.example.squideck.domain

import kotlin.random.Random

/** Decides which card a bot passes. */
fun interface BotStrategy {
    fun chooseCard(legalCards: List<Card>, hand: List<Card>): Card
}

/**
 * Keeps the most-collected symbols and offloads the card that helps the hand least. [mistakeRate] is the
 * fraction of moves played as a random legal card, a knob for easier bots; 0 plays cautiously every time.
 */
class FewestMatchesStrategy(
    private val random: Random = Random.Default,
    private val mistakeRate: Double = 0.0,
) : BotStrategy {
    init {
        require(mistakeRate in 0.0..1.0) { "mistakeRate must be a probability" }
    }

    override fun chooseCard(legalCards: List<Card>, hand: List<Card>): Card {
        if (random.nextDouble() < mistakeRate) return legalCards.random(random)
        val counts = hand.mapNotNull { it.symbol }.groupingBy { it }.eachCount()
        val fewest = legalCards.minOf { counts[it.symbol] ?: 0 }
        return legalCards.filter { (counts[it.symbol] ?: 0) == fewest }.random(random)
    }
}
