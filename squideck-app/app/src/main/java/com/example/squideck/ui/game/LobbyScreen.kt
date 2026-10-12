package com.example.squideck.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.example.squideck.ui.components.GameText
import com.example.squideck.ui.components.GameTopBar
import com.example.squideck.ui.theme.Pink
import com.example.squideck.ui.theme.White

/** The waiting room: you sit alone, then players "join from the internet" one at a time; the round starts by itself. */
@Composable
fun LobbyScreen(
    state: SessionState,
    musicEnabled: Boolean,
    onToggleMusic: () -> Unit,
    onLeave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onLeave)
    val economy = state.economy
    Column(Modifier.then(modifier).fillMaxSize().padding(start = 24.dp, end = 24.dp, bottom = 24.dp)) {
        GameTopBar(onLeave = onLeave, credits = state.balance, musicEnabled = musicEnabled, onToggleMusic = onToggleMusic)
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GameText("${economy.entryCost} table", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            GameText(
                if (!state.isTableFull) "Waiting for players (${state.players.size}/${state.playerCount})"
                else "Table full \u2014 starting\u2026",
                color = if (state.isTableFull) Pink else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (state.isTableFull) FontWeight.Bold else FontWeight.Normal,
            )
            SeatProgress(filled = state.players.size, total = state.playerCount)
            repeat(state.playerCount) { seat ->
                LobbySlot(state.players.getOrNull(seat))
            }
            GameText(
                "1st ${economy.payoutFor(1)}  \u00b7  2nd ${economy.payoutFor(2)}  \u00b7  3rd ${economy.payoutFor(3)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

/** Five segments that light up as seats fill. */
@Composable
private fun SeatProgress(filled: Int, total: Int) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { index ->
            val on by animateFloatAsState(if (index < filled) 1f else 0f, tween(400), label = "seat$index")
            Box(
                Modifier
                    .weight(1f)
                    .height(6.dp)
                    .background(White.copy(alpha = 0.12f), RoundedCornerShape(50))
                    .background(Pink.copy(alpha = on), RoundedCornerShape(50)),
            )
        }
    }
}

@Composable
private fun LobbySlot(player: Player?) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            when {
                player == null -> {
                    EmptyAvatarSlot()
                    GameText("Searching for a player…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                !player.isBot -> {
                    AnimeAvatar(AvatarStyles.byId(player.avatarId), Modifier.size(56.dp).border(2.dp, Pink, CircleShape))
                    GameText(player.name, Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    GameText("YOU", color = Pink, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
                }
                else -> key(player.seat) {
                    var settled by remember { mutableStateOf(false) }
                    // Fades the name in once the reel has stopped on the new player's face.
                    val reveal by animateFloatAsState(if (settled) 1f else 0f, tween(350), label = "reveal")
                    AvatarReel(AvatarStyles.byId(player.avatarId), onSettled = { settled = true })
                    Box(Modifier.weight(1f)) {
                        GameText("Joining\u2026", Modifier.alpha(1f - reveal), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        GameText(
                            player.name,
                            Modifier.alpha(reveal).graphicsLayer { translationX = (1f - reveal) * 24.dp.toPx() },
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    GameText(
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
