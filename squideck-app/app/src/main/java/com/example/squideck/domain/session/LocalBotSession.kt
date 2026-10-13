package com.example.squideck.domain.session

import com.example.squideck.domain.BotStrategy
import com.example.squideck.domain.FewestMatchesStrategy
import com.example.squideck.domain.GameConfig
import com.example.squideck.domain.GameEngine
import com.example.squideck.domain.GameState
import com.example.squideck.domain.MonotonicClock
import com.example.squideck.domain.PassResult
import com.example.squideck.domain.Phase
import com.example.squideck.domain.Player
import com.example.squideck.domain.RandomThinkTime
import com.example.squideck.domain.ThinkTime
import com.example.squideck.domain.TurnTimer
import com.example.squideck.domain.economy.Economy
import com.example.squideck.domain.economy.EconomyConfig
import com.example.squideck.domain.economy.Wallet
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Offline session: the human sits in seat 0 and every other seat is a bot that "joins over the internet"
 * after a random delay. Nobody is host: the round starts by itself once the fifth player has joined.
 * Every turn runs on a timer; if the human lets it expire, a bot plays for them until they take control back.
 */
class LocalBotSession(
    private val scope: CoroutineScope,
    playerName: String,
    private val wallet: Wallet,
    playerAvatarId: Int = 0,
    /** Faces for the four bots, in seat order; the composition root passes the ones the player didn't pick. */
    private val botAvatarIds: List<Int> = List(4) { it + 1 },
    private val config: GameConfig = GameConfig(),
    private val engine: GameEngine = GameEngine(config),
    private val botStrategy: BotStrategy = FewestMatchesStrategy(),
    private val thinkTime: ThinkTime = RandomThinkTime(),
    private val random: Random = Random.Default,
    private val joinDelay: ThinkTime = RandomThinkTime(1_500L..3_500L),
    private val economy: EconomyConfig = EconomyConfig(),
) : GameSession {
    private val localPlayer = Player(seat = LOCAL_SEAT, name = playerName, isBot = false, avatarId = playerAvatarId)
    private val _state = MutableStateFlow(lobbyState())
    override val state: StateFlow<SessionState> = _state.asStateFlow()

    private var turnJob: Job? = null
    private var joinJob: Job? = null
    private var autoPlay = false

    private fun lobbyState() = SessionState(
        players = listOf(localPlayer),
        playerCount = config.playerCount,
        balance = wallet.balance.value,
        economy = economy,
    )

    init {
        fillLobby()
    }

    /** Bots trickle into the waiting room one by one, like real players joining. */
    private fun fillLobby() {
        joinJob?.cancel()
        joinJob = scope.launch {
            BOT_NAMES.take(config.playerCount - 1).forEachIndexed { index, name ->
                delay(joinDelay.nextMs())
                _state.update { it.copy(players = it.players + Player(index + 1, name, isBot = true, avatarId = botAvatarIds[index % botAvatarIds.size])) }
            }
            // A beat for the last avatar reel to land, then the table starts on its own.
            delay(AUTO_START_DELAY_MS)
            val table = _state.value
            if (table.game == null && table.isTableFull && wallet.canAfford(economy.entryCost)) begin(table.players)
        }
    }

    /** Open decision in FEATURES.md: restart keeps the same players for now (local bot mode only). */
    override fun restart() {
        val current = _state.value
        if (current.game != null && current.game.phase != Phase.Playing && current.canAffordEntry) begin(current.players)
    }

    override fun pass(cardId: Int) {
        if (_state.value.isCountingDown) return
        val game = _state.value.game ?: return
        apply(game, LOCAL_SEAT, cardId)
    }

    override fun takeControl() {
        if (!autoPlay) return
        autoPlay = false
        _state.update { it.copy(autoPlay = false) }
        // Give the player a fresh, full turn if the bot was about to move for them.
        if (_state.value.game?.activeSeat == LOCAL_SEAT) beginTurn()
    }

    override fun close() {
        turnJob?.cancel()
        joinJob?.cancel()
    }

    private fun begin(players: List<Player>) {
        turnJob?.cancel()
        autoPlay = false
        wallet.debit(economy.entryCost)
        publish(players, engine.start(players, random))
        // Everyone sees the dealt board and a get-ready countdown; the first turn starts when it ends.
        _state.update { it.copy(countdown = TurnTimer(seat = -1, startedAtMs = MonotonicClock.nowMs(), durationMs = config.startCountdownMs + config.startFlashMs)) }
        turnJob = scope.launch {
            delay(config.startCountdownMs + config.startFlashMs)
            _state.update { it.copy(countdown = null) }
            beginTurn()
        }
    }

    private fun apply(game: GameState, seat: Int, cardId: Int) {
        val result = engine.pass(game, seat, cardId) as? PassResult.Success ?: return
        val settlement = Economy.settle(result.state, economy)
        if (settlement != null) wallet.credit(settlement.payoutFor(LOCAL_SEAT))
        publish(game.players, result.state)
        beginTurn()
    }

    private fun publish(players: List<Player>, game: GameState) {
        _state.value = SessionState(
            players = players,
            game = game,
            legalCardIds = engine.legalCards(game, LOCAL_SEAT).map { it.id }.toSet(),
            recipientSeat = if (game.phase == Phase.Playing) engine.recipientFor(game, LOCAL_SEAT) else null,
            autoPlay = autoPlay,
            localSeat = LOCAL_SEAT,
            playerCount = config.playerCount,
            balance = wallet.balance.value,
            economy = economy,
            settlement = Economy.settle(game, economy),
        )
    }

    /** Starts the clock for the active player and schedules whoever (bot, auto-play or timeout) moves for them. */
    private fun beginTurn() {
        turnJob?.cancel()
        val game = _state.value.game ?: return
        if (game.phase != Phase.Playing) {
            _state.update { it.copy(turnTimer = null) }
            return
        }
        val seat = game.activeSeat
        _state.update { it.copy(turnTimer = TurnTimer(seat, MonotonicClock.nowMs(), config.turnTimeLimitMs)) }
        val botMoves = game.players[seat].isBot || (seat == LOCAL_SEAT && autoPlay)
        turnJob = scope.launch {
            if (botMoves) {
                delay(thinkTime.nextMs().coerceAtMost(config.turnTimeLimitMs - MOVE_MARGIN_MS))
            } else {
                delay(config.turnTimeLimitMs)
                autoPlay = true
                _state.update { it.copy(autoPlay = true) }
            }
            playForSeat(seat)
        }
    }

    private fun playForSeat(seat: Int) {
        val game = _state.value.game ?: return
        if (game.phase != Phase.Playing || game.activeSeat != seat) return
        val card = botStrategy.chooseCard(engine.legalCards(game, seat), game.hands.getValue(seat))
        apply(game, seat, card.id)
    }

    private companion object {
        const val LOCAL_SEAT = 0
        const val MOVE_MARGIN_MS = 500L
        const val AUTO_START_DELAY_MS = 900L
        val BOT_NAMES = listOf("Sakura", "Ren", "Yuki", "Haruto")
    }
}
