package com.example.squideck.domain.session

import com.example.squideck.domain.BotStrategy
import com.example.squideck.domain.FewestMatchesStrategy
import com.example.squideck.domain.GameConfig
import com.example.squideck.domain.GameEngine
import com.example.squideck.domain.GameState
import com.example.squideck.domain.PassResult
import com.example.squideck.domain.Phase
import com.example.squideck.domain.Player
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Offline session: the human is the host in seat 0 and every other seat is a bot. */
class LocalBotSession(
    private val scope: CoroutineScope,
    private val config: GameConfig = GameConfig(),
    private val engine: GameEngine = GameEngine(config),
    private val botStrategy: BotStrategy = FewestMatchesStrategy(),
    private val random: Random = Random.Default,
    private val botJoinDelayMs: Long = 700,
    private val botTurnDelayMs: Long = 1100,
) : GameSession {
    private val host = Player(seat = LOCAL_SEAT, name = "You", isHost = true, isBot = false)
    private val _state = MutableStateFlow(SessionState(players = listOf(host), playerCount = config.playerCount))
    override val state: StateFlow<SessionState> = _state.asStateFlow()

    private var botJob: Job? = null

    init {
        scope.launch {
            BOT_NAMES.take(config.playerCount - 1).forEachIndexed { index, name ->
                delay(botJoinDelayMs)
                _state.update { it.copy(players = it.players + Player(index + 1, name, isHost = false, isBot = true)) }
            }
        }
    }

    override fun start() {
        val current = _state.value
        if (current.canStart) begin(current.players)
    }

    /** Open decision in FEATURES.md: restart keeps the same players for now (local bot mode only). */
    override fun restart() {
        val current = _state.value
        if (current.game != null && current.game.phase != Phase.Playing) begin(current.players)
    }

    override fun pass(cardId: Int) {
        val game = _state.value.game ?: return
        apply(game, LOCAL_SEAT, cardId)
    }

    override fun close() {
        botJob?.cancel()
    }

    private fun begin(players: List<Player>) {
        botJob?.cancel()
        publish(players, engine.start(players, random))
        scheduleBot()
    }

    private fun apply(game: GameState, seat: Int, cardId: Int) {
        val result = engine.pass(game, seat, cardId) as? PassResult.Success ?: return
        publish(game.players, result.state)
        scheduleBot()
    }

    private fun publish(players: List<Player>, game: GameState) {
        _state.value = SessionState(
            players = players,
            game = game,
            legalCardIds = engine.legalCards(game, LOCAL_SEAT).map { it.id }.toSet(),
            recipientSeat = if (game.phase == Phase.Playing) engine.recipientFor(game, LOCAL_SEAT) else null,
            localSeat = LOCAL_SEAT,
            playerCount = config.playerCount,
        )
    }

    private fun scheduleBot() {
        botJob?.cancel()
        val game = _state.value.game ?: return
        val bot = game.players[game.activeSeat]
        if (game.phase != Phase.Playing || !bot.isBot) return
        botJob = scope.launch {
            delay(botTurnDelayMs)
            val card = botStrategy.chooseCard(engine.legalCards(game, bot.seat), game.hands.getValue(bot.seat))
            apply(game, bot.seat, card.id)
        }
    }

    private companion object {
        const val LOCAL_SEAT = 0
        val BOT_NAMES = listOf("Player 067", "Player 218", "Player 456", "Player 199")
    }
}
