package com.example.squideck.domain.session

import com.example.squideck.domain.GameState
import com.example.squideck.domain.Player
import com.example.squideck.domain.TurnTimer
import com.example.squideck.domain.economy.Settlement
import kotlinx.coroutines.flow.StateFlow

/** What the local player can see and do. */
data class SessionState(
    val players: List<Player>,
    val game: GameState? = null,
    /** Cards the local player may pass right now. */
    val legalCardIds: Set<Int> = emptySet(),
    /** Who receives the local player's next pass. */
    val recipientSeat: Int? = null,
    /** Countdown for whoever is on turn; null when no round is running. */
    val turnTimer: TurnTimer? = null,
    /** Set during the get-ready countdown before a round's first turn; nobody can move until it ends. */
    val countdown: TurnTimer? = null,
    /** True while a bot is playing for the local player after a timeout, until they take control back. */
    val autoPlay: Boolean = false,
    val localSeat: Int = 0,
    val playerCount: Int = 5,
    /** Set once a round has finished. */
    val settlement: Settlement? = null,
    /** Set for nearby games: whose room this is and what this phone may do in it. */
    val room: RoomInfo? = null,
    /** Set when the session ended on its own (room closed, connection lost); the UI shows it and leaves. */
    val endedReason: String? = null,
) {
    val isTableFull: Boolean get() = players.size == playerCount
    val isCountingDown: Boolean get() = countdown != null
}

/** A nearby room as seen from one phone. */
data class RoomInfo(
    /** Name of the player who created the room; the room is shown as theirs. */
    val creatorName: String,
    /** True on the room creator's phone. */
    val isCreator: Boolean,
)

/** Who may start or restart a round: anyone at a bot table, only the creator in a nearby room. */
val SessionState.canControlRound: Boolean get() = room?.isCreator ?: true

/**
 * Boundary between UI and game source. [LocalBotSession] runs against bots; a future
 * implementation will drive the same interface from the multiplayer API.
 */
interface GameSession {
    val state: StateFlow<SessionState>

    /** Starts a new round with the same players. Rounds begin on their own when the table fills. */
    fun restart()

    /** Nearby room creator only: start now, with bots in the empty seats. */
    fun startNow() {}
    fun pass(cardId: Int)

    /** Stops auto-play so the local player moves for themselves again. */
    fun takeControl()

    /** Stops the session; the caller returns to the game list. */
    fun close()
}
