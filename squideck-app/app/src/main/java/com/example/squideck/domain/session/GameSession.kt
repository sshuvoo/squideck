package com.example.squideck.domain.session

import com.example.squideck.domain.GameState
import com.example.squideck.domain.Player
import com.example.squideck.domain.TurnTimer
import com.example.squideck.domain.economy.EconomyConfig
import com.example.squideck.domain.economy.Settlement
import com.example.squideck.domain.economy.UsCents
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
    /** True while a bot is playing for the local player after a timeout, until they take control back. */
    val autoPlay: Boolean = false,
    val localSeat: Int = 0,
    val playerCount: Int = 5,
    val balance: UsCents = UsCents(0),
    val economy: EconomyConfig = EconomyConfig(),
    /** Set once a round has finished. */
    val settlement: Settlement? = null,
) {
    val canAffordEntry: Boolean get() = balance >= economy.entryCost
    val isTableFull: Boolean get() = players.size == playerCount
}

/**
 * Boundary between UI and game source. [LocalBotSession] runs against bots; a future
 * implementation will drive the same interface from the multiplayer API.
 */
interface GameSession {
    val state: StateFlow<SessionState>

    /** Starts a new round with the same players. Rounds begin on their own when the table fills; nobody is host. */
    fun restart()
    fun pass(cardId: Int)

    /** Stops auto-play so the local player moves for themselves again. */
    fun takeControl()

    /** Stops the session; the caller returns to the game list. Leaving mid-round forfeits the entry. */
    fun close()
}
