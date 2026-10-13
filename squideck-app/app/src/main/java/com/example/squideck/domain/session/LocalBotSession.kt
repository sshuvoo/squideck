package com.example.squideck.domain.session

import com.example.squideck.domain.BotStrategy
import com.example.squideck.domain.FewestMatchesStrategy
import com.example.squideck.domain.GameConfig
import com.example.squideck.domain.GameEngine
import com.example.squideck.domain.Phase
import com.example.squideck.domain.Player
import com.example.squideck.domain.RandomThinkTime
import com.example.squideck.domain.ThinkTime
import com.example.squideck.domain.economy.Economy
import com.example.squideck.domain.economy.EconomyConfig
import com.example.squideck.domain.economy.Wallet
import com.example.squideck.domain.round.RoundController
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Offline session: the human sits in seat 0 and every other seat is a bot that "joins over the internet"
 * after a random delay. Nobody is host: the round starts by itself once the fifth player has joined.
 * Rounds are run by [RoundController] (timers, bots, auto-play); this class adds the lobby and the credits.
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
    botStrategy: BotStrategy = FewestMatchesStrategy(),
    thinkTime: ThinkTime = RandomThinkTime(),
    random: Random = Random.Default,
    private val joinDelay: ThinkTime = RandomThinkTime(1_500L..3_500L),
    private val economy: EconomyConfig = EconomyConfig(),
) : GameSession {
    private val localPlayer = Player(seat = LOCAL_SEAT, name = playerName, isBot = false, avatarId = playerAvatarId)
    private var players = listOf(localPlayer)

    private val round = RoundController(scope, config, engine, botStrategy, thinkTime, random) { finished ->
        Economy.settle(finished, economy)?.let { wallet.credit(it.payoutFor(LOCAL_SEAT)) }
    }

    private val _state = MutableStateFlow(view())
    override val state: StateFlow<SessionState> = _state.asStateFlow()

    private val joinJob: Job
    private val roundJob: Job

    init {
        roundJob = scope.launch { round.state.collect { _state.value = view() } }
        joinJob = scope.launch { fillLobby() }
    }

    private fun view() = sessionView(players, round.state.value, LOCAL_SEAT, engine, config, wallet.balance.value, economy)

    /** Bots trickle into the waiting room one by one, like real players joining. */
    private suspend fun fillLobby() {
        BOT_NAMES.take(config.playerCount - 1).forEachIndexed { index, name ->
            delay(joinDelay.nextMs())
            players = players + Player(index + 1, name, isBot = true, avatarId = botAvatarIds[index % botAvatarIds.size])
            _state.value = view()
        }
        // A beat for the last avatar reel to land, then the table starts on its own.
        delay(AUTO_START_DELAY_MS)
        if (round.state.value == null && players.size == config.playerCount && wallet.canAfford(economy.entryCost)) begin()
    }

    /** Open decision in FEATURES.md: restart keeps the same players for now (local bot mode only). */
    override fun restart() {
        val game = round.state.value?.game ?: return
        if (game.phase != Phase.Playing && wallet.canAfford(economy.entryCost)) begin()
    }

    override fun pass(cardId: Int) {
        round.pass(LOCAL_SEAT, cardId)
    }

    override fun takeControl() = round.takeControl(LOCAL_SEAT)

    override fun close() {
        joinJob.cancel()
        roundJob.cancel()
        round.close()
    }

    private fun begin() {
        wallet.debit(economy.entryCost)
        round.start(players)
    }

    private companion object {
        const val LOCAL_SEAT = 0
        const val AUTO_START_DELAY_MS = 900L
        val BOT_NAMES = listOf("Sakura", "Ren", "Yuki", "Haruto")
    }
}
