package com.example.squideck.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.squideck.domain.session.SessionState

@Composable
fun LobbyScreen(state: SessionState, onStart: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("SQUIDECK", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
        Text(
            "Waiting for players (${state.players.size}/${state.playerCount})",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        repeat(state.playerCount) { seat ->
            val player = state.players.getOrNull(seat)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        player?.name ?: "Waiting…",
                        modifier = Modifier.weight(1f),
                        color = if (player == null) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface,
                    )
                    if (player?.isHost == true) Text("HOST", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Button(onClick = onStart, enabled = state.canStart, modifier = Modifier.fillMaxWidth()) {
            Text("Start game")
        }
    }
}
