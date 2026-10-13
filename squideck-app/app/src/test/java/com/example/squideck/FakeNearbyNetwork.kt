package com.example.squideck

import com.example.squideck.net.ClientEvent
import com.example.squideck.net.ClientTransport
import com.example.squideck.net.HostEvent
import com.example.squideck.net.HostTransport
import com.example.squideck.net.RoomAd
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow

/** In-memory stand-in for Nearby Connections: one room creator and any number of joining phones. */
class FakeNearbyNetwork {
    val host = FakeHost()
    private var nextPeer = 0

    fun newClient() = FakeClient("peer${nextPeer++}")

    inner class FakeHost : HostTransport {
        private val channel = Channel<HostEvent>(Channel.UNLIMITED)
        override val events: Flow<HostEvent> = channel.receiveAsFlow()
        var advertising = false
        var ad: RoomAd? = null
        val clients = mutableMapOf<String, FakeClient>()

        override fun startAdvertising(creatorName: String, avatarId: Int) {
            advertising = true
            ad = RoomAd("room", creatorName, avatarId)
        }
        override fun stopAdvertising() { advertising = false }
        override fun send(peerId: String, bytes: ByteArray) { clients[peerId]?.receive(bytes) }
        override fun disconnect(peerId: String) { clients.remove(peerId)?.dropped() }
        override fun close() {
            advertising = false
            clients.values.toList().forEach { it.dropped() }
            clients.clear()
        }
        fun fromClient(event: HostEvent) { channel.trySend(event) }
    }

    inner class FakeClient(val peerId: String) : ClientTransport {
        private val channel = Channel<ClientEvent>(Channel.UNLIMITED)
        override val events: Flow<ClientEvent> = channel.receiveAsFlow()
        override val rooms: StateFlow<List<RoomAd>> = MutableStateFlow(listOfNotNull(host.ad))
        override val discoveryError: StateFlow<String?> = MutableStateFlow(null)
        override fun startDiscovery() = Unit
        override fun stopDiscovery() = Unit
        override fun connect(roomId: String, playerName: String) {
            if (!host.advertising) {
                channel.trySend(ClientEvent.Failed("Room not found"))
                return
            }
            host.clients[peerId] = this
            host.fromClient(HostEvent.PeerConnected(peerId))
            channel.trySend(ClientEvent.Connected)
        }
        override fun send(bytes: ByteArray) {
            if (host.clients[peerId] === this) host.fromClient(HostEvent.PeerMessage(peerId, bytes))
        }
        override fun close() {
            if (host.clients.remove(peerId) != null) host.fromClient(HostEvent.PeerLeft(peerId))
        }
        fun receive(bytes: ByteArray) { channel.trySend(ClientEvent.Message(bytes)) }
        fun dropped() { channel.trySend(ClientEvent.Disconnected) }
    }
}
