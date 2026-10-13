package com.example.squideck.net

import com.example.squideck.domain.GameEvent
import com.example.squideck.domain.GameState
import com.example.squideck.domain.Player
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Bumped whenever messages change shape; phones on different versions refuse to play together. */
const val PROTOCOL_VERSION = 1

/** Messages a player's phone sends to the room creator's phone. */
@Serializable
sealed interface ClientMessage {
    @Serializable @SerialName("join")
    data class Join(val name: String, val avatarId: Int, val protocol: Int = PROTOCOL_VERSION) : ClientMessage

    @Serializable @SerialName("pass")
    data class Pass(val cardId: Int) : ClientMessage

    @Serializable @SerialName("take_control")
    data object TakeControl : ClientMessage
}

/** Messages the room creator's phone sends to each player. */
@Serializable
sealed interface HostMessage {
    /** The waiting room, sent whenever someone joins or leaves; [yourSeat] is the receiver's seat. */
    @Serializable @SerialName("lobby")
    data class Lobby(val players: List<Player>, val yourSeat: Int, val creatorName: String) : HostMessage

    /** The table after every change during a round. */
    @Serializable @SerialName("round")
    data class Round(val snapshot: RoundSnapshot, val yourSeat: Int, val players: List<Player>) : HostMessage

    /** The room would not take this player (full, already playing, different app version). */
    @Serializable @SerialName("rejected")
    data class Rejected(val reason: String) : HostMessage

    /** The room creator closed the room. */
    @Serializable @SerialName("closed")
    data object Closed : HostMessage
}

/**
 * A round as sent over the air. Clocks differ between phones, so timers travel as time remaining. The event log
 * grows every move, so only its tail is sent with the total count; each phone appends what it has not seen.
 */
@Serializable
data class RoundSnapshot(
    val game: GameState,
    val eventsTotal: Int,
    val turnSeat: Int? = null,
    val turnRemainingMs: Long = 0,
    val turnDurationMs: Long = 0,
    val countdownRemainingMs: Long? = null,
    val countdownDurationMs: Long = 0,
    val autoPlaySeats: Set<Int> = emptySet(),
)

/** How many of the latest events each snapshot carries; plenty, since snapshots go out after every move. */
const val SNAPSHOT_EVENT_TAIL = 12

/** One JSON setup for both directions; unknown fields are ignored so small additions stay compatible. */
val ProtocolJson = Json {
    ignoreUnknownKeys = true
    classDiscriminator = "type"
}

fun ClientMessage.encode(): ByteArray = ProtocolJson.encodeToString(ClientMessage.serializer(), this).encodeToByteArray()
fun HostMessage.encode(): ByteArray = ProtocolJson.encodeToString(HostMessage.serializer(), this).encodeToByteArray()
fun decodeClientMessage(bytes: ByteArray): ClientMessage? =
    runCatching { ProtocolJson.decodeFromString(ClientMessage.serializer(), bytes.decodeToString()) }.getOrNull()
fun decodeHostMessage(bytes: ByteArray): HostMessage? =
    runCatching { ProtocolJson.decodeFromString(HostMessage.serializer(), bytes.decodeToString()) }.getOrNull()

/** Appends the snapshot's new events to what this phone already has, so card flights and the log stay in step. */
fun mergeEvents(known: List<GameEvent>, snapshot: RoundSnapshot): List<GameEvent> {
    val tail = snapshot.game.events
    val tailStart = snapshot.eventsTotal - tail.size
    if (snapshot.eventsTotal < known.size || tailStart > known.size) return tail // new round, or a gap we cannot fill
    return known + tail.drop(known.size - tailStart)
}
