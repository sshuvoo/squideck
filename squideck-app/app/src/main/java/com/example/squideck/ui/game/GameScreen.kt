package com.example.squideck.ui.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.squideck.domain.GameEvent
import com.example.squideck.domain.GameState
import com.example.squideck.domain.Phase
import com.example.squideck.domain.Player
import com.example.squideck.domain.session.SessionState
import com.example.squideck.ui.theme.Black
import com.example.squideck.ui.theme.Pink

@Composable
fun GameScreen(
    state: GameUiState,
    onSelectCard: (Int) -> Unit,
    onPass: () -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val session = state.session
    val game = session.game ?: return
    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                game.players.filter { it.seat != session.localSeat }.forEach { player ->
                    PlayerChip(player, game, Modifier.weight(1f))
                }
            }
            StatusPanel(session, Modifier.weight(1f).fillMaxWidth())
            HandSection(session, state.selectedCardId, onSelectCard, onPass)
        }
        if (game.phase != Phase.Playing) {
            ResultOverlay(game, onRestart)
        }
    }
}

@Composable
private fun PlayerChip(player: Player, game: GameState, modifier: Modifier = Modifier) {
    val place = game.winners.indexOf(player.seat).takeIf { it >= 0 }?.plus(1)
    val active = game.phase == Phase.Playing && game.activeSeat == player.seat
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (active) 2.dp else 1.dp, if (active) Pink else MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(vertical = 10.dp, horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(player.name.removePrefix("Player "), fontWeight = FontWeight.Bold)
            Text(
                if (place != null) placeLabel(place) else "${game.hands.getValue(player.seat).size} cards",
                style = MaterialTheme.typography.labelSmall,
                color = if (place != null) Pink else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatusPanel(session: SessionState, modifier: Modifier = Modifier) {
    val game = session.game ?: return
    val active = game.players[game.activeSeat]
    val headline = when {
        game.phase != Phase.Playing -> "Round over"
        game.activeSeat == session.localSeat ->
            "Your turn — pass a card to ${game.players[session.recipientSeat ?: 0].name}"
        else -> "${active.name} is choosing…"
    }
    Surface(modifier, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(headline, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            game.events.takeLast(6).reversed().forEach { event ->
                Text(
                    describe(event, game, session.localSeat),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HandSection(session: SessionState, selectedCardId: Int?, onSelectCard: (Int) -> Unit, onPass: () -> Unit) {
    val game = session.game ?: return
    val hand = game.hands.getValue(session.localSeat)
    val myTurn = game.phase == Phase.Playing && game.activeSeat == session.localSeat
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Your hand", color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 16.dp),
        ) {
            items(hand, key = { it.id }) { card ->
                CardView(
                    card = card,
                    selected = card.id == selectedCardId,
                    received = game.lastReceivedCardBySeat[session.localSeat] == card.id,
                    blocked = myTurn && card.id !in session.legalCardIds,
                    enabled = myTurn && card.id in session.legalCardIds,
                    onClick = { onSelectCard(card.id) },
                )
            }
        }
        Button(onClick = onPass, enabled = myTurn && selectedCardId != null, modifier = Modifier.fillMaxWidth()) {
            Text("Pass card")
        }
    }
}

@Composable
private fun ResultOverlay(game: GameState, onRestart: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Black.copy(alpha = 0.8f)).clickable(enabled = false) {},
        contentAlignment = Alignment.Center,
    ) {
        Surface(Modifier.padding(24.dp), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (game.phase == Phase.Completed) "Round complete" else "No legal moves left",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                )
                game.winners.forEachIndexed { index, seat ->
                    Text("${placeLabel(index + 1)}  ${game.players[seat].name}")
                }
                game.losers.forEach { seat ->
                    Text("Lost  ${game.players[seat].name}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) { Text("Play again") }
            }
        }
    }
}

private fun placeLabel(place: Int) = when (place) {
    1 -> "1st"
    2 -> "2nd"
    else -> "3rd"
}

private fun describe(event: GameEvent, game: GameState, localSeat: Int): String {
    fun name(seat: Int) = game.players[seat].name
    return when (event) {
        is GameEvent.Passed -> {
            val visible = event.from == localSeat || event.to == localSeat
            val card = if (visible) " (${event.card.symbol?.name ?: "Phantom"})" else ""
            "${name(event.from)} passed a card$card to ${name(event.to)}"
        }
        is GameEvent.Skipped -> "${name(event.seat)} had no card to pass — skipped"
        is GameEvent.Won -> "${name(event.seat)} finished ${placeLabel(event.place)}"
    }
}
