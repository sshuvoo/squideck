package com.example.squideck.data.nearby

import android.content.Context
import com.example.squideck.net.ClientEvent
import com.example.squideck.net.ClientTransport
import com.example.squideck.net.HostEvent
import com.example.squideck.net.HostTransport
import com.example.squideck.net.RoomAd
import com.example.squideck.net.RoomName
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

/*
 * Google Nearby Connections, the recommended way for phones to play together nearby. It picks Bluetooth, BLE or
 * Wi-Fi by itself and needs no router or internet. STAR topology: the room creator is the hub, friends connect
 * to it (the shape Google recommends for one advertiser and several discoverers).
 */
private const val SERVICE_ID = "com.example.squideck.nearby"
private val STRATEGY: Strategy = Strategy.P2P_STAR

/** Friendly text for Nearby failures, which otherwise arrive as status codes. */
internal fun describe(error: Exception): String {
    val code = (error as? ApiException)?.statusCode
    return when (code) {
        ConnectionsStatusCodes.MISSING_PERMISSION_BLUETOOTH,
        ConnectionsStatusCodes.MISSING_PERMISSION_BLUETOOTH_ADMIN,
        ConnectionsStatusCodes.MISSING_PERMISSION_ACCESS_WIFI_STATE,
        ConnectionsStatusCodes.MISSING_PERMISSION_CHANGE_WIFI_STATE,
        ConnectionsStatusCodes.MISSING_PERMISSION_ACCESS_COARSE_LOCATION,
        -> "Squideck needs the Nearby devices permission."
        ConnectionsStatusCodes.STATUS_ENDPOINT_UNKNOWN -> "That room is no longer nearby."
        ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> "The room didn't let you in."
        ConnectionsStatusCodes.STATUS_ALREADY_ADVERTISING,
        ConnectionsStatusCodes.STATUS_ALREADY_DISCOVERING,
        -> "Already busy with another room. Go back and try again."
        else -> "Nearby play isn't available right now. Check Bluetooth and Wi-Fi are on (${code ?: error.message})."
    }
}

class NearbyHostTransport(context: Context) : HostTransport {
    private val client: ConnectionsClient = Nearby.getConnectionsClient(context.applicationContext)
    private val channel = Channel<HostEvent>(Channel.UNLIMITED)
    override val events: Flow<HostEvent> = channel.receiveAsFlow()
    private val connected = mutableSetOf<String>()

    private val payloads = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            payload.asBytes()?.let { channel.trySend(HostEvent.PeerMessage(endpointId, it)) }
        }
        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) = Unit
    }

    private val lifecycle = object : ConnectionLifecycleCallback() {
        // Anyone nearby may knock; the room seats or turns them away once they say who they are.
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            client.acceptConnection(endpointId, payloads)
        }
        override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution) {
            if (resolution.status.isSuccess) {
                connected += endpointId
                channel.trySend(HostEvent.PeerConnected(endpointId))
            }
        }
        override fun onDisconnected(endpointId: String) {
            connected -= endpointId
            channel.trySend(HostEvent.PeerLeft(endpointId))
        }
    }

    override fun startAdvertising(creatorName: String, avatarId: Int) {
        val options = AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
        client.startAdvertising(RoomName.encode(creatorName, avatarId), SERVICE_ID, lifecycle, options)
            .addOnFailureListener { channel.trySend(HostEvent.Failed(describe(it))) }
    }

    override fun stopAdvertising() = client.stopAdvertising()

    override fun send(peerId: String, bytes: ByteArray) {
        if (peerId in connected) client.sendPayload(peerId, Payload.fromBytes(bytes))
    }

    override fun disconnect(peerId: String) {
        connected -= peerId
        client.disconnectFromEndpoint(peerId)
    }

    override fun close() {
        client.stopAdvertising()
        client.stopAllEndpoints()
        connected.clear()
    }
}

class NearbyClientTransport(context: Context) : ClientTransport {
    private val client: ConnectionsClient = Nearby.getConnectionsClient(context.applicationContext)
    private val channel = Channel<ClientEvent>(Channel.UNLIMITED)
    override val events: Flow<ClientEvent> = channel.receiveAsFlow()

    private val _rooms = MutableStateFlow<List<RoomAd>>(emptyList())
    override val rooms: StateFlow<List<RoomAd>> = _rooms.asStateFlow()
    private val _discoveryError = MutableStateFlow<String?>(null)
    override val discoveryError: StateFlow<String?> = _discoveryError.asStateFlow()

    private var roomId: String? = null
    private var discovering = false

    private val discovery = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            if (info.serviceId != SERVICE_ID) return
            val ad = RoomName.decode(endpointId, info.endpointName) ?: return
            _rooms.update { rooms -> rooms.filter { it.id != endpointId } + ad }
        }
        override fun onEndpointLost(endpointId: String) {
            _rooms.update { rooms -> rooms.filter { it.id != endpointId } }
        }
    }

    private val payloads = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            payload.asBytes()?.let { channel.trySend(ClientEvent.Message(it)) }
        }
        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) = Unit
    }

    private val lifecycle = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            client.acceptConnection(endpointId, payloads)
        }
        override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution) {
            if (resolution.status.isSuccess) channel.trySend(ClientEvent.Connected)
            else channel.trySend(ClientEvent.Failed("Couldn't join the room. Move closer and try again."))
        }
        override fun onDisconnected(endpointId: String) {
            channel.trySend(ClientEvent.Disconnected)
        }
    }

    override fun startDiscovery() {
        if (discovering) return
        discovering = true
        _discoveryError.value = null
        val options = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
        client.startDiscovery(SERVICE_ID, discovery, options).addOnFailureListener {
            discovering = false
            _discoveryError.value = describe(it)
        }
    }

    override fun stopDiscovery() {
        if (!discovering) return
        discovering = false
        client.stopDiscovery()
        _rooms.value = emptyList()
    }

    /** Stops looking first: Nearby recommends it, and connections are faster and steadier without discovery. */
    override fun connect(roomId: String, playerName: String) {
        stopDiscovery()
        this.roomId = roomId
        client.requestConnection(playerName, roomId, lifecycle)
            .addOnFailureListener { channel.trySend(ClientEvent.Failed(describe(it))) }
    }

    override fun send(bytes: ByteArray) {
        roomId?.let { client.sendPayload(it, Payload.fromBytes(bytes)) }
    }

    override fun close() {
        stopDiscovery()
        client.stopAllEndpoints()
        roomId = null
    }
}

/** Creates the real Nearby radios for the app. */
class NearbyRadios(private val context: Context) : com.example.squideck.net.NearbyRadios {
    override fun host(): HostTransport = NearbyHostTransport(context)
    override fun client(): ClientTransport = NearbyClientTransport(context)
}
