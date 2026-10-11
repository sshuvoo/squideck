package com.example.squideck.ui.game

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.squideck.domain.Player
import com.example.squideck.domain.session.SessionState
import com.example.squideck.ui.components.GameButton
import com.example.squideck.ui.components.GameButtonSize
import com.example.squideck.ui.components.GameButtonStyle
import com.example.squideck.ui.theme.Pink

/** The waiting room: you sit alone, then players "join from the internet" one at a time; the round starts by itself. */
@Composable
fun LobbyScreen(state: SessionState, onLeave: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("${state.economy.entryCost} table", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text(
                if (state.players.size < state.playerCount) "Waiting for players (${state.players.size}/${state.playerCount})"
                else "Table full — starting…",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            repeat(state.playerCount) { seat ->
                LobbySlot(state.players.getOrNull(seat))
            }
            Text(
                "Entry ${state.economy.entryCost} · Credits ${state.balance}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        GameButton(
            "Leave", onLeave, Modifier.align(Alignment.TopStart).padding(start = 8.dp, top = 4.dp),
            style = GameButtonStyle.Ghost, size = GameButtonSize.Small, leading = "✕",
        )
    }
}

@Composable
private fun LobbySlot(player: Player?) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            when {
                player == null -> {
                    EmptyAvatarSlot()
                    Text("Searching for a player…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                !player.isBot -> {
                    InitialBadge(player.name)
                    Text(player.name, Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    Text("YOU", color = Pink, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
                }
                else -> key(player.seat) {
                    var settled by remember { mutableStateOf(false) }
                    // Fades the name in once the reel has stopped on the new player's face.
                    val reveal by animateFloatAsState(if (settled) 1f else 0f, tween(350), label = "reveal")
                    AvatarReel(AvatarStyles.forSeat(player.seat), onSettled = { settled = true })
                    Box(Modifier.weight(1f)) {
                        Text("Joining\u2026", Modifier.alpha(1f - reveal), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            player.name,
                            Modifier.alpha(reveal).graphicsLayer { translationX = (1f - reveal) * 24.dp.toPx() },
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        "JOINED",
                        Modifier.alpha(reveal),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

/** The local player has no avatar, so their slot shows the first letter of their name. */
@Composable
private fun InitialBadge(name: String) {
    Box(Modifier.size(56.dp).background(Pink, CircleShape), contentAlignment = Alignment.Center) {
        Text(name.take(1).uppercase(), fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
    }
}
