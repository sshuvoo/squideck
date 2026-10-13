package com.example.squideck.net

import com.example.squideck.domain.BotStrategy
import com.example.squideck.domain.FewestMatchesStrategy
import com.example.squideck.domain.GameConfig
import com.example.squideck.domain.GameEngine
import com.example.squideck.domain.MonotonicClock
import com.example.squideck.domain.Phase
import com.example.squideck.domain.Player
import com.example.squideck.domain.RandomThinkTime
import com.example.squideck.domain.ThinkTime
import com.example.squideck.domain.economy.EconomyConfig
import com.example.squideck.domain.economy.Wallet
import com.example.squideck.domain.round.RoundController
import com.example.squideck.domain.round.RoundState
import com.example.squideck.domain.session.GameSession
import com.example.squideck.domain.session.RoomInfo
import com.example.squideck.domain.session.SessionState
import com.example.squideck.domain.session.sessionView
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The room creator's side of a nearby game. This phone is the authority: it advertises the room, seats friends as
 * they join, fills the empty seats with bots when the creator starts (or starts by itself when five people are in),
 * runs the round with [RoundController] and sends every change to every player. Nearby games are free play.
 */
class HostSession(
    private val scope: CoroutineScope,
    private val playerName: String,
    private val avatarId: Int,
    private val wallet: Wallet,
    private val transport: HostTransport,
    private val config: GameConfig = GameConfig(),
    private val engine: GameEngine = GameEngine(config),
    botStrategy: BotStrategy = FewestMatchesStrategy(),
    thinkTime: ThinkTime = RandomThinkTime(),
    random: Random = Random.Default,
    /** Avatar ids bots may use, in order of preference. */
    private val botAvatarIds: List<Int> = List(5) { it },
    private val clock: () -> Long = MonotonicClock::nowMs,
) : GameSession {
    private val room = RoomInfo(creatorName = playerName, isCreator = true)
    private var players = listOf(Player(seat = CREATOR_SEAT, name = playerName, isBot = false, avatarId = avatarId))

    /** Connected phones that have joined, by peer id -> seat. */
    private val seatsByPeer = linkedMapOf<String, Int>()

    private val round = RoundController(scope, config, engine, botStrategy, thinkTime, random)
    private val _state = MutableStateFlow(view())
    override val state: StateFlow<SessionState> = _state.asStateFlow()

    private val jobs = mutableListOf<Job>()
    private var autoStartJob: Job? = null

    init {
        transport.startAdvertising(playerName, avatarId)
        jobs += scope.launch { transport.events.collect(::onEvent) }
        jobs += scope.launch {
            round.state.collect { roundState ->
                if (roundState != null) broadcastRound(roundState)
                _state.value = view()
            }
        }
    }

    private val inRound: Boolean get() = round.state.value != null

    private fun view() = sessionView(
        players, round.state.value, CREATOR_SEAT, engine, config, wallet.balance.value, EconomyConfig(),
        freePlay = true, room = room,
    )

    override fun startNow() {
        if (inRound) return
        autoStartJob?.cancel()
        transport.stopAdvertising()
        players = players + botsForEmptySeats()
        round.start(players)
    }

    /** A new round with the same seats; players who left are played by bots. */
    override fun restart() {
        val game = round.state.value?.game ?: return
        if (game.phase != Phase.Playing) round.start(players)
    }

    override fun pass(cardId: Int) {
        round.pass(CREATOR_SEAT, cardId)
    }

    override fun takeControl() = round.takeControl(CREATOR_SEAT)

    override fun close() {
        autoStartJob?.cancel()
        seatsByPeer.keys.forEach { transport.send(it, HostMessage.Closed.encode()) }
        jobs.forEach { it.cancel() }
        round.close()
        transport.close()
    }

    private fun onEvent(event: HostEvent) {
        when (event) {
            is HostEvent.PeerConnected -> Unit // Seated once its Join message arrives.
            is HostEvent.PeerMessage -> decodeClientMessage(event.bytes)?.let { onMessage(event.peerId, it) }
            is HostEvent.PeerLeft -> onPeerLeft(event.peerId)
            is HostEvent.Failed -> _state.value = view().copy(endedReason = event.reason)
        }
    }

    private fun onMessage(peerId: String, message: ClientMessage) {
        when (message) {
            is ClientMessage.Join -> seat(peerId, message)
            is ClientMessage.Pass -> seatsByPeer[peerId]?.let { round.pass(it, message.cardId) }
            ClientMessage.TakeControl -> seatsByPeer[peerId]?.let { round.takeControl(it) }
        }
    }

    private fun seat(peerId: String, join: ClientMessage.Join) {
        val reason = when {
            join.protocol != PROTOCOL_VERSION -> "This room is on a different version of Squideck. Update both phones."
            inRound -> "The game in this room has already started."
            players.size >= config.playerCount -> "This room is full."
            else -> null
        }
        if (reason != null) {
            transport.send(peerId, HostMessage.Rejected(reason).encode())
            transport.disconnect(peerId)
            return
        }
        if (peerId in seatsByPeer) return
        val seat = players.size
        seatsByPeer[peerId] = seat
        players = players + Player(seat, join.name.take(MAX_NAME), isBot = false, avatarId = join.avatarId)
        lobbyChanged()
        // Five people in: start by itself, as at the bot table, after a beat for the last avatar to land.
        if (players.size == config.playerCount) {
            autoStartJob = scope.launch {
                delay(AUTO_START_DELAY_MS)
                startNow()
            }
        }
    }

    private fun onPeerLeft(peerId: String) {
        val seat = seatsByPeer.remove(peerId) ?: return
        if (inRound) {
            round.playerLeft(seat)
            return
        }
        // Still waiting: free the seat and close the gap so seats stay 0..n-1.
        autoStartJob?.cancel()
        players = players.filter { it.seat != seat }.mapIndexed { index, player -> player.copy(seat = index) }
        seatsByPeer.replaceAll { _, oldSeat -> if (oldSeat > seat) oldSeat - 1 else oldSeat }
        lobbyChanged()
    }

    private fun lobbyChanged() {
        seatsByPeer.forEach { (peer, seat) -> transport.send(peer, HostMessage.Lobby(players, seat, playerName).encode()) }
        _state.value = view()
    }

    private fun broadcastRound(roundState: RoundState) {
        val snapshot = roundState.toSnapshot(clock())
        seatsByPeer.forEach { (peer, seat) -> transport.send(peer, HostMessage.Round(snapshot, seat, players).encode()) }
    }

    private fun botsForEmptySeats(): List<Player> {
        val usedAvatars = players.map { it.avatarId }.toSet()
        val usedNames = players.map { it.name.lowercase() }.toSet()
        val avatars = (botAvatarIds.filter { it !in usedAvatars } + botAvatarIds).iterator()
        val names = BOT_NAMES.filter { it.lowercase() !in usedNames }.iterator()
        return (players.size until config.playerCount).map { seat ->
            Player(seat, if (names.hasNext()) names.next() else "Bot ${seat + 1}", isBot = true, avatarId = avatars.next())
        }
    }

    private companion object {
        const val CREATOR_SEAT = 0
        const val MAX_NAME = 14
        const val AUTO_START_DELAY_MS = 900L
        val BOT_NAMES = listOf("Sakura", "Ren", "Yuki", "Haruto", "Mei")
    }
}
