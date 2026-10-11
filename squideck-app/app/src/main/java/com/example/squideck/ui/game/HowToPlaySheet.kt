package com.example.squideck.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.squideck.domain.Symbol
import com.example.squideck.domain.economy.EconomyConfig
import com.example.squideck.ui.components.GameButton
import com.example.squideck.ui.theme.Pink

/** The rules in six steps, with the real numbers from the current table. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowToPlaySheet(economy: EconomyConfig, turnSeconds: Long, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("How to play", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Symbol.entries.forEach { SymbolIcon(it, Modifier.size(28.dp)) }
                SymbolIcon(null, Modifier.size(28.dp), color = Pink)
            }
            Rule(1, "Collect four of a kind", "Get four cards with the same symbol. Five players, four symbols in play each round.")
            Rule(2, "Pass one card each turn", "On your turn, pass one card to the next player clockwise. You have $turnSeconds seconds.")
            Rule(3, "Fresh cards are blocked", "The card you just received, and every card with the same symbol, can't be passed on your next move.")
            Rule(4, "Win on your own move", "You win when, after your pass, you hold four matching cards. The first three players to do it win.")
            Rule(5, "Beware the Imposter", "It matches nothing. Whoever is dealt it goes first but can't pass it on that first move.")
            Rule(
                6,
                "Prizes",
                "Entry ${economy.entryCost}. 1st wins ${economy.payoutFor(1)}, 2nd ${economy.payoutFor(2)}, 3rd ${economy.payoutFor(3)}. " +
                    "The other two get nothing. If time runs out, a bot plays for you until you tap.",
            )
            GameButton("Got it", onDismiss, Modifier.fillMaxWidth().padding(bottom = 16.dp))
        }
    }
}

@Composable
private fun Rule(number: Int, title: String, body: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(28.dp).background(Pink, CircleShape), contentAlignment = Alignment.Center) {
            Text("$number", fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelLarge)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
