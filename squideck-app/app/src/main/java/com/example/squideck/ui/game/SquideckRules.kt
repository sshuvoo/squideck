package com.example.squideck.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.squideck.domain.GameConfig
import com.example.squideck.domain.economy.EconomyConfig
import com.example.squideck.ui.components.GameText
import com.example.squideck.ui.theme.Ink
import com.example.squideck.ui.theme.Pink
import com.example.squideck.ui.theme.White

/** Rules of the Squideck card-passing game, with the real numbers of the table they belong to. */
fun squideckRules(economy: EconomyConfig, config: GameConfig = GameConfig()) = GameRules(
    title = "${economy.entryCost} table",
    details = { TableDetails(economy, config) },
    // The tiles above carry every number (players, winners, entry, turn time, prizes); the rules don't repeat them.
    rules = listOf(
        RuleItem("Collect four of a kind", "Hold four cards with the same symbol."),
        RuleItem("Swipe to pass", "On your turn, swipe one card up to the next player."),
        RuleItem("Fresh cards are blocked", "You can't pass the symbol you just received."),
        RuleItem("Beware the Imposter", "It matches nothing. Its holder goes first but can't pass it on that move."),
    ),
)

/** The table's facts and prizes, at the top of the info sheet. */
@Composable
private fun TableDetails(economy: EconomyConfig, config: GameConfig) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Fact("Players", "${config.playerCount}", Modifier.weight(1f))
            Fact("Winners", "${config.winnerSlots}", Modifier.weight(1f))
            Fact("Entry", economy.entryCost.toString(), Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Fact("1st", economy.payoutFor(1).toString(), Modifier.weight(1f), highlight = true)
            Fact("2nd", economy.payoutFor(2).toString(), Modifier.weight(1f))
            Fact("3rd", economy.payoutFor(3).toString(), Modifier.weight(1f))
        }
    }
}

@Composable
private fun Fact(label: String, value: String, modifier: Modifier = Modifier, highlight: Boolean = false) {
    Column(
        modifier.background(Ink.copy(alpha = 0.28f), RoundedCornerShape(14.dp)).padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GameText(label, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        GameText(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = if (highlight) Pink else White)
    }
}
