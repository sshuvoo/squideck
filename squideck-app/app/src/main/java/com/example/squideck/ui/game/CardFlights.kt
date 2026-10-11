package com.example.squideck.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.squideck.domain.Card
import com.example.squideck.domain.GameEvent
import com.example.squideck.domain.GameState
import com.example.squideck.domain.Symbol
import com.example.squideck.ui.theme.MaskBlack
import com.example.squideck.ui.theme.Pink
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

private const val FLIGHT_MS = 650
private val CardWidth = 76.dp
private val CardHeight = 108.dp
private val ArcHeight = 36.dp
/** Hand cards are drawn a bit smaller than the flying card, so the flight starts/ends near hand size. */
private const val HandScale = 0.8f

data class CardFlight(val id: Int, val from: Int, val to: Int, val card: Card, val localSeat: Int) {
    val involvesLocal: Boolean get() = from == localSeat || to == localSeat
}

/** Where things are on screen (in root coordinates) plus the cards currently in the air. */
@Stable
class CardFlightState {
    private val seatCenters = mutableStateMapOf<Int, Offset>()
    private val cardCenters = mutableStateMapOf<Int, Offset>()
    val flights = mutableStateListOf<CardFlight>()
    private var nextId = 0

    fun updateSeat(seat: Int, center: Offset) {
        if (seatCenters[seat] != center) seatCenters[seat] = center
    }

    fun updateCard(cardId: Int, center: Offset) {
        if (cardCenters[cardId] != center) cardCenters[cardId] = center
    }

    /** Cards flying into the local hand; the hand keeps their slot empty until they land. */
    fun arrivingCardIds(localSeat: Int): Set<Int> =
        flights.filter { it.to == localSeat }.map { it.card.id }.toSet()

    fun launch(event: GameEvent.Passed, localSeat: Int) {
        flights += CardFlight(nextId++, event.from, event.to, event.card, localSeat)
    }

    fun land(flight: CardFlight) {
        flights.remove(flight)
    }

    fun startOf(flight: CardFlight): Offset? =
        if (flight.from == flight.localSeat) cardCenters[flight.card.id] ?: seatCenters[flight.from] else seatCenters[flight.from]

    fun endOf(flight: CardFlight): Offset? =
        if (flight.to == flight.localSeat) cardCenters[flight.card.id] ?: seatCenters[flight.to] else seatCenters[flight.to]
}

/** Starts a flight for every new pass in [game]; events that already existed when first shown are not replayed. */
@Composable
fun CardFlightEffect(game: GameState, localSeat: Int, state: CardFlightState) {
    var processed by remember { mutableIntStateOf(game.events.size) }
    LaunchedEffect(game.events.size) {
        if (game.events.size < processed) processed = 0
        for (index in processed until game.events.size) {
            (game.events[index] as? GameEvent.Passed)?.let { state.launch(it, localSeat) }
        }
        processed = game.events.size
    }
}

/** Full-screen, touch-transparent layer that draws the cards in flight. */
@Composable
fun CardFlightLayer(state: CardFlightState, modifier: Modifier = Modifier) {
    var origin by remember { mutableStateOf(Offset.Zero) }
    Box(modifier.fillMaxSize().onGloballyPositioned { origin = it.positionInRoot() }) {
        state.flights.forEach { flight ->
            key(flight.id) { FlyingCard(flight, state, origin) }
        }
    }
}

@Composable
private fun FlyingCard(flight: CardFlight, state: CardFlightState, origin: Offset) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(FLIGHT_MS, easing = FastOutSlowInEasing))
        state.land(flight)
    }
    val toLocal = flight.to == flight.localSeat
    val fromLocal = flight.from == flight.localSeat
    Box(
        Modifier
            .offset {
                val start = state.startOf(flight)
                val end = state.endOf(flight)
                if (start == null || end == null) return@offset IntOffset(-10_000, -10_000)
                val t = progress.value
                val x = start.x + (end.x - start.x) * t
                val y = start.y + (end.y - start.y) * t - ArcHeight.toPx() * sin(PI * t).toFloat()
                IntOffset((x - origin.x - CardWidth.toPx() / 2).roundToInt(), (y - origin.y - CardHeight.toPx() / 2).roundToInt())
            }
            .size(CardWidth, CardHeight)
            .graphicsLayer {
                val t = progress.value
                val from = if (fromLocal) HandScale else 0.45f
                val to = if (toLocal) HandScale else 0.4f
                val s = from + (to - from) * t
                scaleX = s
                scaleY = s
                rotationZ = sin(PI * t).toFloat() * 10f * (if (toLocal) -1f else 1f)
                alpha = when {
                    t < 0.08f && !fromLocal -> t / 0.08f
                    t > 0.85f && !toLocal -> (1f - t) / 0.15f
                    else -> 1f
                }
            },
    ) {
        if (flight.involvesLocal) {
            CardView(flight.card, selected = false, received = false, blocked = false, enabled = true, onClick = {})
        } else {
            CardBack()
        }
    }
}

/** Face-down card shown when the symbol is none of the local player's business. */
@Composable
fun CardBack(modifier: Modifier = Modifier) {
    Surface(
        modifier.size(CardWidth, CardHeight),
        shape = RoundedCornerShape(14.dp),
        color = MaskBlack,
        border = BorderStroke(1.5.dp, Pink),
    ) {
        Box(contentAlignment = Alignment.Center) {
            SymbolIcon(Symbol.Circle, Modifier.size(34.dp), color = Pink)
        }
    }
}
