package com.example.squideck.domain

import kotlin.random.Random

/** Decides which card a bot passes. */
fun interface BotStrategy {
    fun chooseCard(legalCards: List<Card>, hand: List<Card>): Card
}

/** Keeps the most-collected symbols and offloads the card that helps the hand least. */
class FewestMatchesStrategy(private val random: Random = Random.Default) : BotStrategy {
    override fun chooseCard(legalCards: List<Card>, hand: List<Card>): Card {
        val counts = hand.mapNotNull { it.symbol }.groupingBy { it }.eachCount()
        val fewest = legalCards.minOf { counts[it.symbol] ?: 0 }
        return legalCards.filter { (counts[it.symbol] ?: 0) == fewest }.random(random)
    }
}
