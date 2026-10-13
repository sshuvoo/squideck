package com.example.squideck.domain.session

import com.example.squideck.domain.BotStrategy
import com.example.squideck.domain.FewestMatchesStrategy
import com.example.squideck.domain.GameConfig
import com.example.squideck.domain.GameEngine
import com.example.squideck.domain.Phase
import com.example.squideck.domain.Player
import com.example.squideck.domain.RandomThinkTime
import com.example.squideck.domain.ThinkTime
import com.example.squideck.domain.round.RoundController
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Offline session: the human sits in seat 0 and the other four seats are bots. There is no waiting room (that is
 * for online play): the table is full from the start and the round begins at once, so the player goes straight to
 * the game board. Rounds are run by [RoundController] (timers, bots, auto-play). Bot tables are free play: no
 * credits, entry or prizes (credits are for online play only).
 */
class LocalBotSession(
    private val scope: CoroutineScope,
    playerName: String,
    playerAvatarId: Int = 0,
    /** Faces for the four bots, in seat order; the composition root passes the ones the player didn't pick. */
    private val botAvatarIds: List<Int> = List(4) { it + 1 },
    private val config: GameConfig = GameConfig(),
    private val engine: GameEngine = GameEngine(config),
    botStrategy: BotStrategy = FewestMatchesStrategy(),
    thinkTime: ThinkTime = RandomThinkTime(),
    random: Random = Random.Default,
) : GameSession {
    private val players = listOf(Player(seat = LOCAL_SEAT, name = playerName, isBot = false, avatarId = playerAvatarId)) +
        BOT_NAMES.take(config.playerCount - 1).mapIndexed { index, name ->
            Player(index + 1, name, isBot = true, avatarId = botAvatarIds[index % botAvatarIds.size])
        }

    // Started before the first state is published, so the lobby never shows.
    private val round = RoundController(scope, config, engine, botStrategy, thinkTime, random).also { it.start(players) }

    private val _state = MutableStateFlow(view())
    override val state: StateFlow<SessionState> = _state.asStateFlow()

    private val roundJob: Job = scope.launch { round.state.collect { _state.value = view() } }

    private fun view() = sessionView(players, round.state.value, LOCAL_SEAT, engine, config)

    /** Open decision in FEATURES.md: restart keeps the same players for now (local bot mode only). */
    override fun restart() {
        val game = round.state.value?.game ?: return
        if (game.phase != Phase.Playing) begin()
    }

    override fun pass(cardId: Int) {
        round.pass(LOCAL_SEAT, cardId)
    }

    override fun takeControl() = round.takeControl(LOCAL_SEAT)

    override fun close() {
        roundJob.cancel()
        round.close()
    }

    private fun begin() = round.start(players)

    private companion object {
        const val LOCAL_SEAT = 0
        val BOT_NAMES = listOf("Sakura", "Ren", "Yuki", "Haruto")
    }
}
