package com.example.squideck.net

import com.example.squideck.domain.GameConfig
import com.example.squideck.domain.GameEngine
import com.example.squideck.domain.MonotonicClock
import com.example.squideck.domain.Player
import com.example.squideck.domain.economy.EconomyConfig
import com.example.squideck.domain.economy.Wallet
import com.example.squideck.domain.round.RoundState
import com.example.squideck.domain.session.GameSession
import com.example.squideck.domain.session.RoomInfo
import com.example.squideck.domain.session.SessionState
import com.example.squideck.domain.session.sessionView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * A joining player's side of a nearby game. The room creator's phone runs the game; this phone mirrors the table
 * it sends, works out its own legal cards for the UI, and sends moves back. Nearby games are free play.
 */
class ClientSession(
    scope: CoroutineScope,
    private val playerName: String,
    private val avatarId: Int,
    private val wallet: Wallet,
    private val transport: ClientTransport,
    roomId: String,
    creatorName: String,
    private val config: GameConfig = GameConfig(),
    private val engine: GameEngine = GameEngine(config),
    private val clock: () -> Long = MonotonicClock::nowMs,
) : GameSession {
    private var room = RoomInfo(creatorName = creatorName, isCreator = false)
    private var players = emptyList<Player>()
    private var seat = 0
    private var round: RoundState? = null
    private var endedReason: String? = null

    private val _state = MutableStateFlow(view())
    override val state: StateFlow<SessionState> = _state.asStateFlow()

    private val job: Job = scope.launch { transport.events.collect(::onEvent) }

    init {
        transport.connect(roomId, playerName)
    }

    private fun view(): SessionState = sessionView(
        players, round, seat, engine, config, wallet.balance.value, EconomyConfig(), freePlay = true, room = room,
    ).copy(endedReason = endedReason)

    private fun publish() {
        _state.value = view()
    }

    private fun onEvent(event: ClientEvent) {
        when (event) {
            ClientEvent.Connected -> transport.send(ClientMessage.Join(playerName, avatarId).encode())
            is ClientEvent.Message -> decodeHostMessage(event.bytes)?.let(::onMessage)
            is ClientEvent.Failed -> end(event.reason)
            ClientEvent.Disconnected -> end("Lost connection to ${room.creatorName}'s room.")
        }
    }

    private fun onMessage(message: HostMessage) {
        when (message) {
            is HostMessage.Lobby -> {
                players = message.players
                seat = message.yourSeat
                room = room.copy(creatorName = message.creatorName)
                round = null
            }
            is HostMessage.Round -> {
                players = message.players
                seat = message.yourSeat
                round = message.snapshot.toRoundState(round?.game?.events.orEmpty(), clock())
            }
            is HostMessage.Rejected -> endedReason = message.reason
            HostMessage.Closed -> endedReason = "${room.creatorName} closed the room."
        }
        publish()
    }

    private fun end(reason: String) {
        if (endedReason == null) endedReason = reason
        publish()
    }

    override fun pass(cardId: Int) = transport.send(ClientMessage.Pass(cardId).encode())

    override fun takeControl() = transport.send(ClientMessage.TakeControl.encode())

    /** Only the room creator starts rounds. */
    override fun restart() = Unit

    override fun close() {
        job.cancel()
        transport.close()
    }
}
