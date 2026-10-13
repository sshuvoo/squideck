package com.example.squideck.domain.round

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
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Everything about a running round that every player's screen needs. */
data class RoundState(
    val game: GameState,
    /** Countdown for whoever is on turn; null during the get-ready countdown and after the round ends. */
    val turnTimer: TurnTimer? = null,
    /** The 3-2-1-START countdown before the first turn; nobody can move while it runs. */
    val countdown: TurnTimer? = null,
    /** Seats a bot is currently playing for: human seats whose turn timed out, and players who left. */
    val autoPlaySeats: Set<Int> = emptySet(),
)

/**
 * Runs rounds authoritatively: deals, plays the get-ready countdown, times every turn, moves bots after a random
 * think time, and lets a bot take over a human seat whose time runs out until that player takes control again.
 * Used by the bot game and by the room creator's phone in a nearby game, so both follow exactly the same rules.
 */
class RoundController(
    private val scope: CoroutineScope,
    private val config: GameConfig = GameConfig(),
    private val engine: GameEngine = GameEngine(config),
    private val botStrategy: BotStrategy = FewestMatchesStrategy(),
    private val thinkTime: ThinkTime = RandomThinkTime(),
    private val random: Random = Random.Default,
    /** Called once, with the final state, when a round ends. */
    private val onRoundEnded: (GameState) -> Unit = {},
) {
    private val _state = MutableStateFlow<RoundState?>(null)
    val state: StateFlow<RoundState?> = _state.asStateFlow()

    private var turnJob: Job? = null

    /** Seats whose player has left the room; a bot plays them for the rest of the round with no turn timeout. */
    private val awaySeats = mutableSetOf<Int>()

    fun start(players: List<Player>) {
        turnJob?.cancel()
        val countdownMs = config.startCountdownMs + config.startFlashMs
        _state.value = RoundState(
            game = engine.start(players, random),
            countdown = TurnTimer(seat = -1, startedAtMs = MonotonicClock.nowMs(), durationMs = countdownMs),
            autoPlaySeats = awaySeats.toSet(),
        )
        turnJob = scope.launch {
            delay(countdownMs)
            _state.update { it?.copy(countdown = null) }
            beginTurn()
        }
    }

    /** A move from [seat]; ignored unless it is legal right now. Returns whether it was applied. */
    fun pass(seat: Int, cardId: Int): Boolean {
        val round = _state.value ?: return false
        if (round.countdown != null) return false
        return apply(round.game, seat, cardId)
    }

    /** [seat]'s player is back in control after a timeout; they get a fresh, full turn if it is theirs. */
    fun takeControl(seat: Int) {
        val round = _state.value ?: return
        if (seat !in round.autoPlaySeats || seat in awaySeats) return
        _state.value = round.copy(autoPlaySeats = round.autoPlaySeats - seat)
        if (round.countdown == null && round.game.activeSeat == seat) beginTurn()
    }

    /** [seat]'s player left: a bot plays for them from now on. */
    fun playerLeft(seat: Int) {
        awaySeats += seat
        val round = _state.value ?: return
        _state.value = round.copy(autoPlaySeats = round.autoPlaySeats + seat)
        if (round.countdown == null && round.game.activeSeat == seat) beginTurn()
    }

    fun close() {
        turnJob?.cancel()
    }

    private fun apply(game: GameState, seat: Int, cardId: Int): Boolean {
        val result = engine.pass(game, seat, cardId) as? PassResult.Success ?: return false
        // Settle first so the final screen already shows the payout.
        if (result.state.phase != Phase.Playing) onRoundEnded(result.state)
        _state.update { it?.copy(game = result.state) }
        beginTurn()
        return true
    }

    /** Starts the clock for the active seat and schedules whoever (bot, auto-play or timeout) moves for it. */
    private fun beginTurn() {
        turnJob?.cancel()
        val round = _state.value ?: return
        val game = round.game
        if (game.phase != Phase.Playing) {
            _state.value = round.copy(turnTimer = null)
            return
        }
        val seat = game.activeSeat
        _state.value = round.copy(turnTimer = TurnTimer(seat, MonotonicClock.nowMs(), config.turnTimeLimitMs))
        val botMoves = game.players[seat].isBot || seat in round.autoPlaySeats
        turnJob = scope.launch {
            if (botMoves) {
                delay(thinkTime.nextMs().coerceAtMost(config.turnTimeLimitMs - MOVE_MARGIN_MS))
            } else {
                delay(config.turnTimeLimitMs)
                _state.update { it?.copy(autoPlaySeats = it.autoPlaySeats + seat) }
            }
            playForSeat(seat)
        }
    }

    private fun playForSeat(seat: Int) {
        val game = _state.value?.game ?: return
        if (game.phase != Phase.Playing || game.activeSeat != seat) return
        val card = botStrategy.chooseCard(engine.legalCards(game, seat), game.hands.getValue(seat))
        apply(game, seat, card.id)
    }

    private companion object {
        const val MOVE_MARGIN_MS = 500L
    }
}
