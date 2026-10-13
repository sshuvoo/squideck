package com.example.squideck.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Game models are @Serializable so the room creator can send the table to nearby players (see net/Protocol.kt).

@Serializable
enum class Symbol { Star, Square, Triangle, Circle, Umbrella }

/** A regular card has a [symbol]; the Imposter has none. */
@Serializable
data class Card(val id: Int, val symbol: Symbol?) {
    val isImposter: Boolean get() = symbol == null
}

/** [avatarId] picks the player's face; it travels with the player so everyone sees the same avatar. */
@Serializable
data class Player(val seat: Int, val name: String, val isBot: Boolean, val avatarId: Int = 0)

@Serializable
enum class Phase { Playing, Completed, Stalemate }

@Serializable
sealed interface GameEvent {
    @Serializable @SerialName("passed")
    data class Passed(val from: Int, val to: Int, val card: Card) : GameEvent
    @Serializable @SerialName("skipped")
    data class Skipped(val seat: Int) : GameEvent
    @Serializable @SerialName("won")
    data class Won(val seat: Int, val place: Int) : GameEvent
}

@Serializable
data class GameState(
    val players: List<Player>,
    val hands: Map<Int, List<Card>>,
    /** Seat -> id of the card that seat received most recently; that card and any card of the same symbol cannot be passed by its holder. */
    val lastReceivedCardBySeat: Map<Int, Int>,
    val activeSeat: Int,
    val imposterHolder: Int,
    val imposterHolderHasMoved: Boolean,
    /** Seats in finishing order: index 0 = first place. */
    val winners: List<Int>,
    val phase: Phase,
    val events: List<GameEvent>,
) {
    val losers: List<Int>
        get() = if (phase == Phase.Completed) players.map { it.seat } - winners.toSet() else emptyList()
}

sealed interface PassResult {
    data class Success(val state: GameState) : PassResult
    data class Rejected(val reason: PassError) : PassResult
}

enum class PassError { NotPlaying, NotYourTurn, CardNotOwned, CardJustReceived, ImposterFirstMove }
