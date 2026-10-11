package com.example.squideck.domain.session

import com.example.squideck.domain.GameState
import com.example.squideck.domain.Player
import kotlinx.coroutines.flow.StateFlow

/** What the local player can see and do. */
data class SessionState(
    val players: List<Player>,
    val game: GameState? = null,
    /** Cards the local player may pass right now. */
    val legalCardIds: Set<Int> = emptySet(),
    /** Who receives the local player's next pass. */
    val recipientSeat: Int? = null,
    val localSeat: Int = 0,
    val playerCount: Int = 5,
) {
    val canStart: Boolean get() = game == null && players.size == playerCount
}

/**
 * Boundary between UI and game source. [LocalBotSession] runs against bots; a future
 * implementation will drive the same interface from the multiplayer API.
 */
interface GameSession {
    val state: StateFlow<SessionState>
    fun start()
    fun restart()
    fun pass(cardId: Int)
    fun close()
}
