package com.example.squideck.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.squideck.domain.Card
import com.example.squideck.domain.GameConfig
import com.example.squideck.domain.GameEvent
import com.example.squideck.domain.GameState
import com.example.squideck.domain.Phase
import com.example.squideck.domain.Player
import com.example.squideck.domain.Symbol
import com.example.squideck.domain.session.SessionState
import com.example.squideck.ui.components.ConfirmDialog
import com.example.squideck.ui.components.GameButton
import com.example.squideck.ui.components.GameButtonSize
import com.example.squideck.ui.components.GameButtonStyle
import com.example.squideck.ui.components.GameText
import com.example.squideck.ui.components.GameTopBar
import com.example.squideck.ui.theme.Ink
import com.example.squideck.ui.theme.Pink
import com.example.squideck.ui.theme.White
import kotlinx.coroutines.delay

private const val WIN_BANNER_MS = 2_400L

@Composable
fun GameScreen(
    session: SessionState,
    onPassCard: (Int) -> Unit,
    onTapCard: () -> Unit,
    onRestart: () -> Unit,
    onLeave: () -> Unit,
    musicEnabled: Boolean,
    onToggleMusic: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val game = session.game ?: return
    val flights = remember { CardFlightState() }
    CardFlightEffect(game, session.localSeat, flights)
    KeepScreenOn()
    YourTurnHaptic(game.phase == Phase.Playing && !session.isCountingDown && game.activeSeat == session.localSeat)
    val banner = rememberWinBanner(game, session.localSeat)
    var confirmLeave by rememberSaveable { mutableStateOf(false) }
    val roundOver = game.phase != Phase.Playing
    // Mid-round, leaving forfeits the round, so ask first; after the round it is just navigation.
    val requestLeave = { if (roundOver) onLeave() else confirmLeave = true }
    BackHandler(onBack = requestLeave)
    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            GameTopBar(onLeave = requestLeave, credits = session.balance, musicEnabled = musicEnabled, onToggleMusic = onToggleMusic)
            PlayerTable(
                seats = game.players.map { seatInfo(it, game, session) },
                localSeat = session.localSeat,
                onSeatPositioned = flights::updateSeat,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) { TableCenter(session) }
            HandSection(session, flights, onPassCard, onTapCard)
        }
        CardFlightLayer(flights)
        // The results card already announces the final places, so banners stop once the round is over.
        if (!roundOver) WinBanner(banner, Modifier.align(Alignment.TopCenter).padding(top = 64.dp))
        session.countdown?.let { StartCountdown(it, flashMs = GameConfig().startFlashMs) }
        if (roundOver) {
            ResultOverlay(session, onRestart, onLeave)
        }
        if (confirmLeave) {
            ConfirmDialog(
                title = "Leave game?",
                message = "You will leave this round and return to the game list. Your entry is not refunded.",
                confirmLabel = "Leave",
                dismissLabel = "Stay",
                onConfirm = { confirmLeave = false; onLeave() },
                onDismiss = { confirmLeave = false },
            )
        }
    }
}

/** Turns are timed, so the screen must not dim mid-game. */
@Composable
private fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

@Composable
private fun YourTurnHaptic(myTurn: Boolean) {
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(myTurn) {
        if (myTurn) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
}

private fun seatInfo(player: Player, game: GameState, session: SessionState): SeatInfo {
    val place = game.winners.indexOf(player.seat).takeIf { it >= 0 }?.plus(1)
    val myTurn = game.phase == Phase.Playing && !session.isCountingDown && game.activeSeat == session.localSeat
    return SeatInfo(
        player = player,
        subtitle = if (place != null) placeLabel(place) else "${game.hands.getValue(player.seat).size} cards",
        timer = session.turnTimer?.takeIf { it.seat == player.seat && game.phase == Phase.Playing },
        isFinished = place != null,
        isLocal = player.seat == session.localSeat,
        isRecipient = myTurn && player.seat == session.recipientSeat,
    )
}

@Composable
private fun TableCenter(session: SessionState) {
    val game = session.game ?: return
    val myTurn = game.phase == Phase.Playing && !session.isCountingDown && game.activeSeat == session.localSeat
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        when {
            session.isCountingDown -> GameText("Get ready", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            game.phase != Phase.Playing -> GameText("Round over", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            myTurn -> {
                val pulse by rememberInfiniteTransition(label = "yourTurn").animateFloat(
                    initialValue = 1f,
                    targetValue = 1.07f,
                    animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
                    label = "pulse",
                )
                GameText(
                    "YOUR TURN",
                    Modifier.scale(pulse),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = Pink,
                )
                GameText(
                    "Pass a card to ${game.players[session.recipientSeat ?: 0].name}",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                )
            }
            else -> GameText(
                "${game.players[game.activeSeat].name} is choosing…",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
        Column(Modifier.padding(top = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            game.events.takeLast(2).reversed().forEachIndexed { index, event ->
                GameText(
                    describe(event, game, session.localSeat),
                    Modifier.alpha(if (index == 0) 1f else 0.6f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private const val MIN_HAND_SLOTS = 5
private val HandGap = 6.dp

/** Delay between cards when the hand re-centres, so they slide one by one. */
private const val SLIDE_STAGGER_MS = 70L

/** Matching cards sit together; the Imposter goes last. */
private val handOrder = compareBy<Card>({ it.isImposter }, { it.symbol?.ordinal ?: 0 }, { it.id })

private data class HandHint(val text: String, val emphasis: Boolean = false)

/** One line under the hand that explains what the player can do right now, and why cards are blocked. */
private fun handHint(session: SessionState, game: GameState, hand: List<Card>): HandHint {
    val seat = session.localSeat
    val place = game.winners.indexOf(seat).takeIf { it >= 0 }?.plus(1)
    val myTurn = game.phase == Phase.Playing && !session.isCountingDown && game.activeSeat == seat
    val received = hand.firstOrNull { it.id == game.lastReceivedCardBySeat[seat] }
    return when {
        place != null -> HandHint("You finished ${placeLabel(place)}. Watch the rest of the round.")
        session.autoPlay -> HandHint("A bot is playing for you. Tap a card to take over.", emphasis = true)
        myTurn && received?.symbol != null ->
            HandHint("You received ${received.symbol.withArticle()}, so ${received.symbol.plural()} are blocked. Swipe another card up.", emphasis = true)
        myTurn && received != null -> HandHint("You received the Imposter, so it is blocked. Swipe another card up.", emphasis = true)
        myTurn && hand.any { it.isImposter } && game.imposterHolder == seat && !game.imposterHolderHasMoved ->
            HandHint("You start! Swipe a card up to pass it. The Imposter can't go on your first move.", emphasis = true)
        myTurn -> HandHint("Swipe a card up to pass it to ${game.players[session.recipientSeat ?: 0].name}.", emphasis = true)
        else -> {
            val best = hand.mapNotNull { it.symbol }.groupingBy { it }.eachCount().maxByOrNull { it.value }
            if (best != null && best.value >= 3) HandHint("${best.value} of 4 ${best.key.plural()}: one more to win. Keep them!")
            else HandHint("Collect four of a kind to win.")
        }
    }
}

private fun Symbol.plural() = "${name}s"

/** "a Circle", "an Umbrella". */
private fun Symbol.withArticle() = "${if (name.first().lowercaseChar() in "aeiou") "an" else "a"} $name"

@Composable
private fun HandSection(
    session: SessionState,
    flights: CardFlightState,
    onPassCard: (Int) -> Unit,
    onTapCard: () -> Unit,
) {
    val game = session.game ?: return
    val unsorted = game.hands.getValue(session.localSeat)
    val hand = remember(unsorted) { unsorted.sortedWith(handOrder) }
    val myTurn = game.phase == Phase.Playing && !session.isCountingDown && game.activeSeat == session.localSeat
    val autoPlaying = session.autoPlay && game.phase == Phase.Playing && session.localSeat !in game.winners
    val hint = handHint(session, game, hand)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        GameText(
            "YOUR HAND",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        GameText(
            hint.text,
            style = MaterialTheme.typography.bodySmall,
            color = if (hint.emphasis) Pink else MaterialTheme.colorScheme.onSurface,
        )
        // Cards shrink to share the row, so the whole hand is always visible without scrolling.
        BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 6.dp)) {
            val slots = maxOf(hand.size, MIN_HAND_SLOTS)
            val cardWidth = minOf(CardBaseWidth, (maxWidth - HandGap * (slots - 1)) / slots)
            val arriving = flights.arrivingCardIds(session.localSeat)
            // Centred slots; when the hand changes, cards glide to their new slot one after another.
            val rowWidth = cardWidth * hand.size + HandGap * (hand.size - 1).coerceAtLeast(0)
            val startX = (maxWidth - rowWidth) / 2
            Box(
                Modifier.fillMaxWidth().height(cardWidth * (CardBaseHeight / CardBaseWidth)).onGloballyPositioned {
                    flights.updateSeat(session.localSeat, it.positionInRoot() + Offset(it.size.width / 2f, it.size.height / 2f))
                },
            ) {
                val recipient = game.players[session.recipientSeat ?: 0].name
                hand.forEachIndexed { index, card ->
                    key(card.id) {
                        val passable = myTurn && card.id in session.legalCardIds
                        val slotX = startX + (cardWidth + HandGap) * index
                        // New arrivals start in place (they are hidden until their flight lands); the rest slide.
                        val x = remember { Animatable(slotX, Dp.VectorConverter) }
                        LaunchedEffect(slotX) {
                            if (x.value != slotX) {
                                delay(SLIDE_STAGGER_MS * index)
                                x.animateTo(slotX, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow))
                            }
                        }
                        Box(Modifier.offset { IntOffset(x.value.roundToPx(), 0) }) {
                        ThrowableCard(
                            card = card,
                            received = game.lastReceivedCardBySeat[session.localSeat] == card.id,
                            blocked = myTurn && !passable,
                            enabled = passable || autoPlaying,
                            throwable = passable,
                            width = cardWidth,
                            passLabel = "Pass to $recipient",
                            onThrow = { onPassCard(card.id) },
                            onTap = onTapCard,
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
        }
    }
}

private class WinBannerState(val text: String, val visible: Boolean)

/** Watches for new wins and shows a short banner for each one ("Sakura finished 1st"). */
@Composable
private fun rememberWinBanner(game: GameState, localSeat: Int): WinBannerState {
    var processed by remember { mutableIntStateOf(game.events.size) }
    var text by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(game.events.size) {
        if (game.events.size < processed) processed = 0
        val won = game.events.subList(processed, game.events.size).filterIsInstance<GameEvent.Won>().lastOrNull()
        processed = game.events.size
        if (won != null) {
            text = if (won.seat == localSeat) "You finished ${placeLabel(won.place)}!" else "${game.players[won.seat].name} finished ${placeLabel(won.place)}"
            visible = true
        }
    }
    LaunchedEffect(text, visible) {
        if (visible) {
            delay(WIN_BANNER_MS)
            visible = false
        }
    }
    return WinBannerState(text, visible)
}

@Composable
private fun WinBanner(state: WinBannerState, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        state.visible,
        modifier,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
    ) {
        Surface(shape = RoundedCornerShape(50), color = Pink, shadowElevation = 8.dp) {
            Row(
                Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SymbolIcon(Symbol.Star, Modifier.size(18.dp), color = White)
                GameText(state.text.uppercase(), fontWeight = FontWeight.Black, letterSpacing = 1.sp, color = White)
            }
        }
    }
}

@Composable
private fun ResultOverlay(session: SessionState, onRestart: () -> Unit, onLeave: () -> Unit) {
    val game = session.game ?: return
    val settlement = session.settlement ?: return
    val myPlace = game.winners.indexOf(session.localSeat) + 1
    val won = myPlace > 0 && !settlement.refunded
    val net = settlement.netFor(session.localSeat)
    Box(
        Modifier.fillMaxSize().background(Ink.copy(alpha = 0.82f)).clickable(enabled = false) {},
        contentAlignment = Alignment.Center,
    ) {
        if (won) Confetti(Modifier.fillMaxSize())
        Surface(Modifier.padding(24.dp), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                GameText(
                    when {
                        settlement.refunded -> "ROUND CANCELLED"
                        won -> "${placeLabel(myPlace).uppercase()} PLACE"
                        else -> "YOU LOST"
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = if (won) Pink else MaterialTheme.colorScheme.onSurface,
                )
                GameText(
                    if (settlement.refunded) "No legal moves were left, so every entry was refunded."
                    else "${net.toSignedString()} this round · Credits ${session.balance}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    settlement.places.forEachIndexed { index, seat ->
                        val me = seat == session.localSeat
                        val player = game.players[seat]
                        val rowShape = RoundedCornerShape(12.dp)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .then(if (me) Modifier.background(Pink.copy(alpha = 0.10f), rowShape).border(1.5.dp, Pink.copy(alpha = 0.7f), rowShape) else Modifier)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            GameText(
                                if (settlement.refunded) "•" else if (seat in game.winners) placeLabel(index + 1) else "–",
                                // Wide enough for "1ST"/"2ND"/"3RD" in the game font, kept on one line.
                                Modifier.width(52.dp),
                                maxLines = 1,
                                softWrap = false,
                                fontWeight = FontWeight.Black,
                                color = if (seat in game.winners) Pink else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            AnimeAvatar(AvatarStyles.byId(player.avatarId), Modifier.size(30.dp))
                            GameText(if (me) "You" else player.name, Modifier.weight(1f), fontWeight = if (me) FontWeight.Black else FontWeight.Normal)
                            GameText(settlement.payoutFor(seat).toString(), fontWeight = FontWeight.Bold, color = if (me) Pink else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                GameButton(
                    if (session.canAffordEntry) "Play again · ${session.economy.entryCost}" else "Not enough credits",
                    onRestart, Modifier.fillMaxWidth(), enabled = session.canAffordEntry,
                )
                GameButton("Leave table", onLeave, Modifier.fillMaxWidth(), style = GameButtonStyle.Ghost, size = GameButtonSize.Small)
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
    fun name(seat: Int) = if (seat == localSeat) "You" else game.players[seat].name
    return when (event) {
        is GameEvent.Passed -> {
            val visible = event.from == localSeat || event.to == localSeat
            val card = if (visible) " (${event.card.symbol?.name ?: "Imposter"})" else ""
            "${name(event.from)} → ${name(event.to)}$card"
        }
        is GameEvent.Skipped -> "${name(event.seat)} skipped"
        is GameEvent.Won -> "${name(event.seat)} finished ${placeLabel(event.place)}"
    }
}
