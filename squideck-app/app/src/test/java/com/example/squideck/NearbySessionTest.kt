package com.example.squideck

import com.example.squideck.domain.GameConfig
import com.example.squideck.domain.GameEvent
import com.example.squideck.domain.GameEngine
import com.example.squideck.domain.Phase
import com.example.squideck.domain.ThinkTime
import com.example.squideck.net.ClientMessage
import com.example.squideck.net.ClientSession
import com.example.squideck.net.HostMessage
import com.example.squideck.net.HostSession
import com.example.squideck.net.RoomName
import com.example.squideck.net.decodeClientMessage
import com.example.squideck.net.decodeHostMessage
import com.example.squideck.net.encode
import kotlin.random.Random
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NearbySessionTest {
    private val config = GameConfig()
    private val engine = GameEngine(config)
    private val fastBots = ThinkTime { 100L }
    private val countdown = config.startCountdownMs + config.startFlashMs

    private fun TestScope.host(net: FakeNearbyNetwork) = HostSession(
        backgroundScope, "Shuvo", 4, net.host, config, engine, thinkTime = fastBots, random = Random(7),
        clock = { testScheduler.currentTime },
    )

    private fun TestScope.client(net: FakeNearbyNetwork, name: String) = ClientSession(
        backgroundScope, name, 1, net.newClient(), "room", "Shuvo", config, engine,
        clock = { testScheduler.currentTime },
    )

    @Test
    fun friendJoinsAndBothPhonesSeeTheRoom() = runTest {
        val net = FakeNearbyNetwork()
        val host = host(net)
        val friend = client(net, "Mina")
        runCurrent()
        assertTrue(net.host.advertising)
        assertEquals(listOf("Shuvo", "Mina"), host.state.value.players.map { it.name })
        assertEquals(listOf("Shuvo", "Mina"), friend.state.value.players.map { it.name })
        assertEquals(1, friend.state.value.localSeat)
        assertTrue(host.state.value.room!!.isCreator)
        assertFalse(friend.state.value.room!!.isCreator)
        assertNull(host.state.value.game)
    }

    @Test
    fun creatorStartsWithBotsAndTheRoundReachesBothPhones() = runTest {
        val net = FakeNearbyNetwork()
        val host = host(net)
        val friend = client(net, "Mina")
        runCurrent()
        host.startNow()
        runCurrent()
        assertFalse(net.host.advertising)
        val game = host.state.value.game!!
        assertEquals(5, game.players.size)
        assertEquals(3, game.players.count { it.isBot })
        assertNotNull(friend.state.value.countdown)
        assertEquals(game.hands, friend.state.value.game!!.hands)
        assertTrue("nobody moves during the countdown", friend.state.value.legalCardIds.isEmpty())
        advanceTimeBy(countdown + 1)
        runCurrent()
        assertNull(friend.state.value.countdown)
        assertNotNull(friend.state.value.turnTimer)
    }

    @Test
    fun friendsMoveIsAppliedByTheHostAndSeenEverywhere() = runTest {
        val net = FakeNearbyNetwork()
        val host = host(net)
        val friend = client(net, "Mina")
        runCurrent()
        host.startNow()
        advanceTimeBy(countdown + 1)
        // Let bots play until it is Mina's turn (seat 1).
        var guard = 0
        while (friend.state.value.game!!.activeSeat != 1 && guard++ < 200) {
            if (host.state.value.game!!.activeSeat == 0) host.pass(host.state.value.legalCardIds.first())
            advanceTimeBy(150)
            runCurrent()
        }
        assertEquals(1, friend.state.value.game!!.activeSeat)
        val before = host.state.value.game!!.events.size
        val card = friend.state.value.legalCardIds.first()
        friend.pass(card)
        runCurrent()
        val passed = host.state.value.game!!.events.drop(before).filterIsInstance<GameEvent.Passed>().first()
        assertEquals(1, passed.from)
        assertEquals(card, passed.card.id)
        assertEquals(host.state.value.game!!.events.size, friend.state.value.game!!.events.size)
    }

    @Test
    fun aRoundPlaysToTheEndOverTheNetworkWithoutCredits() = runTest {
        val net = FakeNearbyNetwork()
        val host = host(net)
        val friend = client(net, "Mina")
        runCurrent()
        host.startNow()
        // Humans time out and bots take over, so the round finishes on its own.
        advanceTimeBy(30 * 60_000L)
        runCurrent()
        val game = host.state.value.game!!
        assertTrue(game.phase != Phase.Playing)
        assertEquals(game.phase, friend.state.value.game!!.phase)
        assertEquals(game.winners, friend.state.value.game!!.winners)
        assertEquals(game.winners, friend.state.value.settlement!!.places.take(game.winners.size))
    }

    @Test
    fun friendLeavingTheLobbyFreesTheSeat() = runTest {
        val net = FakeNearbyNetwork()
        val host = host(net)
        val mina = client(net, "Mina")
        val ken = client(net, "Ken")
        runCurrent()
        assertEquals(3, host.state.value.players.size)
        mina.close()
        runCurrent()
        assertEquals(listOf("Shuvo", "Ken"), host.state.value.players.map { it.name })
        assertEquals(1, ken.state.value.localSeat)
        assertEquals(listOf(0, 1), ken.state.value.players.map { it.seat })
    }

    @Test
    fun friendLeavingMidRoundIsPlayedByABot() = runTest {
        val net = FakeNearbyNetwork()
        val host = host(net)
        val mina = client(net, "Mina")
        runCurrent()
        host.startNow()
        runCurrent()
        mina.close()
        runCurrent()
        advanceTimeBy(countdown + 1)
        runCurrent()
        assertTrue(1 in host.state.value.players.indices)
        // Mina's seat is now auto-played, so the round keeps going without her.
        advanceTimeBy(30 * 60_000L)
        runCurrent()
        assertTrue(host.state.value.game!!.phase != Phase.Playing)
    }

    @Test
    fun fifthPlayerStartsTheRoomByItself() = runTest {
        val net = FakeNearbyNetwork()
        val host = host(net)
        val friends = listOf("A1", "B2", "C3", "D4").map { client(net, it) }
        runCurrent()
        assertNull(host.state.value.game)
        advanceTimeBy(1_000)
        runCurrent()
        val game = host.state.value.game
        assertNotNull(game)
        assertEquals(0, game!!.players.count { it.isBot })
        assertEquals(4, friends.last().state.value.localSeat)
    }

    @Test
    fun aSixthPhoneIsTurnedAway() = runTest {
        val net = FakeNearbyNetwork()
        host(net)
        repeat(4) { client(net, "P$it") }
        runCurrent()
        val late = client(net, "Late")
        runCurrent()
        assertNotNull(late.state.value.endedReason)
    }

    @Test
    fun closingTheRoomTellsEveryone() = runTest {
        val net = FakeNearbyNetwork()
        val host = host(net)
        val friend = client(net, "Mina")
        runCurrent()
        host.close()
        runCurrent()
        assertTrue(friend.state.value.endedReason!!.contains("closed"))
    }

    @Test
    fun messagesRoundTripAndRoomNamesParse() {
        val join = ClientMessage.Join("Mina", 3)
        assertEquals(join, decodeClientMessage(join.encode()))
        assertEquals(HostMessage.Closed, decodeHostMessage(HostMessage.Closed.encode()))
        assertNull(decodeHostMessage("garbage".encodeToByteArray()))
        val ad = RoomName.decode("id", RoomName.encode("Shu|vo", 2))!!
        assertEquals("Shu|vo", ad.creatorName)
        assertEquals(2, ad.avatarId)
        assertNull(RoomName.decode("id", "someone else's app"))
    }
}
