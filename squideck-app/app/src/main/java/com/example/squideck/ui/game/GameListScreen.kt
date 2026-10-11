package com.example.squideck.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.squideck.ui.components.GameButton
import com.example.squideck.ui.components.GameButtonSize
import com.example.squideck.ui.components.GameButtonStyle

/** Lists the tables you can sit at. For now there is a single 10-cent table. */
@Composable
fun GameListScreen(
    state: GameUiState,
    onJoin: () -> Unit,
    onChangeName: () -> Unit,
    onRefillWallet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val economy = state.economy
    Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Hi, ${state.playerName}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("Credits ${state.credits}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            GameButton("Change name", onChangeName, style = GameButtonStyle.Ghost, size = GameButtonSize.Small)
        }
        Text("Choose a table", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${economy.entryCost} table", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text("5 players · 3 winners", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "1st ${economy.payoutFor(1)}  ·  2nd ${economy.payoutFor(2)}  ·  3rd ${economy.payoutFor(3)}",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                GameButton("Join · ${economy.entryCost}", onJoin, Modifier.fillMaxWidth(), enabled = state.canAffordEntry)
                if (!state.canAffordEntry) {
                    Text("Not enough credits to play.", color = MaterialTheme.colorScheme.primary)
                    GameButton("Add demo credits", onRefillWallet, style = GameButtonStyle.Secondary, size = GameButtonSize.Small)
                }
            }
        }
    }
}
