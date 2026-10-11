package com.example.squideck.ui.game

import androidx.compose.foundation.BorderStroke
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.squideck.domain.GameEvent
import com.example.squideck.domain.GameState
import com.example.squideck.domain.Phase
import com.example.squideck.domain.Player
import com.example.squideck.domain.TurnTimer
import com.example.squideck.domain.session.SessionState
import com.example.squideck.ui.theme.Black
import com.example.squideck.ui.theme.Pink

@Composable
fun GameScreen(
    state: GameUiState,
    onSelectCard: (Int) -> Unit,
    onPass: () -> Unit,
    onRestart: () -> Unit,
    onTakeControl: () -> Unit,
    onLeave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val session = state.session
    val game = session.game ?: return
    val flights = remember { CardFlightState() }
    CardFlightEffect(game, session.localSeat, flights)
    var confirmLeave by rememberSaveable { mutableStateOf(false) }
    val roundOver = game.phase != Phase.Playing
    // Mid-round, leaving forfeits the round, so ask first; after the round it is just navigation.
    val requestLeave = { if (roundOver) onLeave() else confirmLeave = true }
    BackHandler(onBack = requestLeave)
    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            PlayerTable(
                opponents = game.players.filter { it.seat != session.localSeat }.map { seatInfo(it, game, session.turnTimer) },
                localSeat = session.localSeat,
                seatCount = game.players.size,
                onSeatPositioned = flights::updateSeat,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) { TableCenter(session) }
            HandSection(session, state.selectedCardId, flights, onSelectCard, onPass, onTakeControl)
        }
        CardFlightLayer(flights)
        TextButton(onClick = requestLeave, modifier = Modifier.align(Alignment.TopStart).padding(start = 4.dp)) {
            Text("\u2715  Leave", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            "Balance ${session.balance}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 14.dp, end = 16.dp),
        )
        if (roundOver) {
            ResultOverlay(session, onRestart, onLeave)
        }
        if (confirmLeave) {
            LeaveDialog(onConfirm = { confirmLeave = false; onLeave() }, onDismiss = { confirmLeave = false })
        }
    }
}

private fun seatInfo(player: Player, game: GameState, timer: TurnTimer?): SeatInfo {
    val place = game.winners.indexOf(player.seat).takeIf { it >= 0 }?.plus(1)
    return SeatInfo(
        player = player,
        subtitle = if (place != null) placeLabel(place) else "${game.hands.getValue(player.seat).size} cards",
        timer = timer?.takeIf { it.seat == player.seat && game.phase == Phase.Playing },
        isFinished = place != null,
    )
}

@Composable
private fun TableCenter(session: SessionState) {
    val game = session.game ?: return
    val active = game.players[game.activeSeat]
    val headline = when {
        game.phase != Phase.Playing -> "Round over"
        game.activeSeat == session.localSeat ->
            "Your turn \u2192 pass to ${game.players[session.recipientSeat ?: 0].name.removePrefix("Player ")}"
        else -> "${active.name.removePrefix("Player ")} is choosing\u2026"
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(headline, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        game.events.takeLast(2).reversed().forEach { event ->
            Text(
                describe(event, game, session.localSeat),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private const val MIN_HAND_SLOTS = 5
private val HandGap = 6.dp

@Composable
private fun HandSection(
    session: SessionState,
    selectedCardId: Int?,
    flights: CardFlightState,
    onSelectCard: (Int) -> Unit,
    onPass: () -> Unit,
    onTakeControl: () -> Unit,
) {
    val game = session.game ?: return
    val hand = game.hands.getValue(session.localSeat)
    val myTurn = game.phase == Phase.Playing && game.activeSeat == session.localSeat
    val autoPlaying = session.autoPlay && game.phase == Phase.Playing && session.localSeat !in game.winners
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Your hand", color = MaterialTheme.colorScheme.onSurfaceVariant)
        // Cards shrink to share the row, so the whole hand is always visible without scrolling.
        BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 16.dp)) {
            val slots = maxOf(hand.size, MIN_HAND_SLOTS)
            val cardWidth = minOf(CardBaseWidth, (maxWidth - HandGap * (slots - 1)) / slots)
            val arriving = flights.arrivingCardIds(session.localSeat)
            Row(
                Modifier.fillMaxWidth().height(cardWidth * (CardBaseHeight / CardBaseWidth)).onGloballyPositioned {
                    flights.updateSeat(session.localSeat, it.positionInRoot() + Offset(it.size.width / 2f, it.size.height / 2f))
                },
                horizontalArrangement = Arrangement.spacedBy(HandGap, Alignment.CenterHorizontally),
            ) {
                hand.forEach { card ->
                    key(card.id) {
                        CardView(
                            card = card,
                            selected = card.id == selectedCardId,
                            received = game.lastReceivedCardBySeat[session.localSeat] == card.id,
                            blocked = myTurn && card.id !in session.legalCardIds,
                            enabled = (myTurn && card.id in session.legalCardIds) || autoPlaying,
                            onClick = { onSelectCard(card.id) },
                            width = cardWidth,
                            modifier = Modifier
                                .onGloballyPositioned {
                                    flights.updateCard(card.id, it.positionInRoot() + Offset(it.size.width / 2f, it.size.height / 2f))
                                }
                                .alpha(if (card.id in arriving) 0f else 1f),
                        )
                    }
                }
            }
        }
        val finishedPlace = game.winners.indexOf(session.localSeat).takeIf { it >= 0 }?.plus(1)
        when {
            finishedPlace != null && game.phase == Phase.Playing -> Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                Text("You finished ${placeLabel(finishedPlace)} \u00b7 watching")
            }
            autoPlaying -> Button(onClick = onTakeControl, modifier = Modifier.fillMaxWidth()) {
                Text("Auto-playing for you \u00b7 tap to take control")
            }
            myTurn -> CountdownButton(session.turnTimer, selectedCardId != null, onPass)
            else -> Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                Text("Waiting for ${game.players[game.activeSeat].name}\u2026")
            }
        }
    }
}

@Composable
private fun ResultOverlay(session: SessionState, onRestart: () -> Unit, onLeave: () -> Unit) {
    val game = session.game ?: return
    val settlement = session.settlement ?: return
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
                settlement.places.forEachIndexed { index, seat ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        val me = seat == session.localSeat
                        Text(
                            "${if (settlement.refunded) "\u2022" else if (seat in game.winners) placeLabel(index + 1) else "Lost"}  ${game.players[seat].name}",
                            fontWeight = if (me) FontWeight.Bold else FontWeight.Normal,
                        )
                        Text(settlement.payoutFor(seat).toString(), color = if (me) Pink else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                val net = settlement.netFor(session.localSeat)
                Text(
                    if (settlement.refunded) "Round cancelled \u00b7 entry refunded"
                    else "Net ${net.toSignedString()} \u00b7 Balance ${session.balance}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onRestart, enabled = session.canAffordEntry, modifier = Modifier.fillMaxWidth()) {
                    Text(if (session.canAffordEntry) "Play again (${session.economy.entryCost})" else "Not enough balance")
                }
                TextButton(onClick = onLeave, modifier = Modifier.fillMaxWidth()) {
                    Text("Leave", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun placeLabel(place: Int) = when (place) {
    1 -> "1st"
    2 -> "2nd"
    3 -> "3rd"
    else -> "${place}th"
}

private fun describe(event: GameEvent, game: GameState, localSeat: Int): String {
    fun name(seat: Int) = game.players[seat].name.removePrefix("Player ")
    return when (event) {
        is GameEvent.Passed -> {
            val visible = event.from == localSeat || event.to == localSeat
            val card = if (visible) " (${event.card.symbol?.name ?: "Phantom"})" else ""
            "${name(event.from)} \u2192 ${name(event.to)}$card"
        }
        is GameEvent.Skipped -> "${name(event.seat)} skipped"
        is GameEvent.Won -> "${name(event.seat)} finished ${placeLabel(event.place)}"
    }
}

/** Pass button that doubles as the local player's turn timer: its fill drains as time runs out. */
@Composable
private fun CountdownButton(timer: TurnTimer?, canPass: Boolean, onPass: () -> Unit) {
    val remaining = rememberRemainingMs(timer)
    val seconds by remember(timer) { derivedStateOf { secondsLeft(remaining.value) } }
    val duration = timer?.durationMs ?: 1L
    val shape = RoundedCornerShape(50)
    Box(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .clickable(enabled = canPass, onClick = onPass)
            .drawBehind {
                val left = remaining.value
                drawRect(countdownColor(left).copy(alpha = 0.25f))
                drawRect(
                    countdownColor(left).copy(alpha = if (canPass) 1f else 0.55f),
                    size = Size(size.width * left / duration, size.height),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        val urgent = remaining.value <= URGENT_MS
        Text(
            "${if (canPass) "Pass card" else "Choose a card"} \u00b7 ${seconds}s",
            fontWeight = FontWeight.Bold,
            color = if (urgent && canPass) Black else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun LeaveDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Leave game?", fontWeight = FontWeight.Bold) },
        text = { Text("You will leave this round and return to the lobby.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Leave", color = Pink, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Stay") } },
    )
}
