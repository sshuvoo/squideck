package com.example.squideck.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.squideck.domain.GameConfig
import com.example.squideck.domain.Symbol
import com.example.squideck.domain.economy.EconomyConfig
import com.example.squideck.ui.theme.Pink

/** Rules of the Squideck card-passing game, with the real numbers of the table they belong to. */
fun squideckRules(economy: EconomyConfig, config: GameConfig = GameConfig()) = GameRules(
    title = "How to play",
    header = {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Symbol.entries.forEach { SymbolIcon(it, Modifier.size(28.dp)) }
            SymbolIcon(null, Modifier.size(28.dp), color = Pink)
        }
    },
    rules = listOf(
        RuleItem("Collect four of a kind", "Get four cards with the same symbol. ${config.playerCount} players, ${config.setCount} symbols in play each round."),
        RuleItem(
            "Swipe a card up to pass it",
            "On your turn, drag or flick one card up to pass it to the next player clockwise. You have ${config.turnTimeLimitMs / 1000} seconds.",
        ),
        RuleItem("Fresh cards are blocked", "The card you just received, and every card with the same symbol, can't be passed on your next move."),
        RuleItem("Win on your own move", "You win when, after your pass, you hold four matching cards. The first ${config.winnerSlots} players to do it win."),
        RuleItem("Beware the Imposter", "It matches nothing. Whoever is dealt it goes first but can't pass it on that first move."),
        RuleItem(
            "Prizes",
            "Entry ${economy.entryCost}. 1st wins ${economy.payoutFor(1)}, 2nd ${economy.payoutFor(2)}, 3rd ${economy.payoutFor(3)}. " +
                "The others get nothing. If time runs out, a bot plays for you until you tap.",
        ),
    ),
)
