package com.example.squideck.net

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** A room someone nearby is advertising. */
data class RoomAd(val id: String, val creatorName: String, val avatarId: Int)

/** What happens on the room creator's side of the connection. */
sealed interface HostEvent {
    data class PeerConnected(val peerId: String) : HostEvent
    data class PeerMessage(val peerId: String, val bytes: ByteArray) : HostEvent
    data class PeerLeft(val peerId: String) : HostEvent
    /** The room could not be advertised, e.g. Bluetooth is off. */
    data class Failed(val reason: String) : HostEvent
}

/** What happens on a joining player's side of the connection. */
sealed interface ClientEvent {
    data object Connected : ClientEvent
    data class Message(val bytes: ByteArray) : ClientEvent
    data class Failed(val reason: String) : ClientEvent
    data object Disconnected : ClientEvent
}

/** The room creator's radio: advertises the room and talks to everyone who joins. */
interface HostTransport {
    val events: Flow<HostEvent>
    fun startAdvertising(creatorName: String, avatarId: Int)
    fun stopAdvertising()
    fun send(peerId: String, bytes: ByteArray)
    fun disconnect(peerId: String)
    fun close()
}

/** A joining player's radio: finds rooms nearby and talks to the one it joined. */
interface ClientTransport {
    val rooms: StateFlow<List<RoomAd>>
    val events: Flow<ClientEvent>
    /** Set when looking for rooms fails, e.g. Bluetooth is off. */
    val discoveryError: StateFlow<String?>
    fun startDiscovery()
    fun stopDiscovery()
    fun connect(roomId: String, playerName: String)
    fun send(bytes: ByteArray)
    fun close()
}

/** Encodes a player as an advertised endpoint name, e.g. "sq1|4|Shuvo". */
object RoomName {
    private const val PREFIX = "sq$PROTOCOL_VERSION"

    fun encode(creatorName: String, avatarId: Int) = "$PREFIX|$avatarId|$creatorName"

    fun decode(id: String, endpointName: String): RoomAd? {
        val parts = endpointName.split('|', limit = 3)
        if (parts.size != 3 || parts[0] != PREFIX) return null
        return RoomAd(id, creatorName = parts[2], avatarId = parts[1].toIntOrNull() ?: 0)
    }
}

/** Makes the radios for a nearby game; swapped for fakes in tests. */
interface NearbyRadios {
    fun host(): HostTransport
    fun client(): ClientTransport
}
