package com.example.squideck.domain

enum class Symbol { Star, Square, Triangle, Circle, Umbrella }

/** A regular card has a [symbol]; the Phantom has none. */
data class Card(val id: Int, val symbol: Symbol?) {
    val isPhantom: Boolean get() = symbol == null
}

data class Player(val seat: Int, val name: String, val isHost: Boolean, val isBot: Boolean)

enum class Phase { Playing, Completed, Stalemate }

sealed interface GameEvent {
    data class Passed(val from: Int, val to: Int, val card: Card) : GameEvent
    data class Skipped(val seat: Int) : GameEvent
    data class Won(val seat: Int, val place: Int) : GameEvent
}

data class GameState(
    val players: List<Player>,
    val hands: Map<Int, List<Card>>,
    /** Seat -> id of the card that seat received most recently; that card and any card of the same symbol cannot be passed by its holder. */
    val lastReceivedCardBySeat: Map<Int, Int>,
    val activeSeat: Int,
    val phantomHolder: Int,
    val phantomHolderHasMoved: Boolean,
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

enum class PassError { NotPlaying, NotYourTurn, CardNotOwned, CardJustReceived, PhantomFirstMove }
