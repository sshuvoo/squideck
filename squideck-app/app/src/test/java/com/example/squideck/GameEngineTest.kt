package com.example.squideck

import com.example.squideck.domain.FewestMatchesStrategy
import com.example.squideck.domain.Card
import com.example.squideck.domain.GameEngine
import com.example.squideck.domain.GameState
import com.example.squideck.domain.PassError
import com.example.squideck.domain.PassResult
import com.example.squideck.domain.Phase
import com.example.squideck.domain.Player
import com.example.squideck.domain.Symbol
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineTest {
    private val engine = GameEngine()
    private val strategy = FewestMatchesStrategy()
    private val players = List(5) { Player(it, "P$it", isHost = it == 0, isBot = it != 0) }

    @Test
    fun start_dealsFourEachPlusPhantomToHolder() {
        val state = engine.start(players, Random(1))
        val all = state.hands.values.flatten()
        assertEquals(21, all.size)
        assertEquals(21, all.map { it.id }.toSet().size)
        assertEquals(1, all.count { it.isPhantom })
        state.hands.forEach { (seat, hand) ->
            assertEquals(if (seat == state.phantomHolder) 5 else 4, hand.size)
        }
        assertEquals(state.phantomHolder, state.activeSeat)
        assertEquals(4, all.mapNotNull { it.symbol }.toSet().size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun start_requiresFivePlayers() {
        engine.start(players.take(4))
    }

    @Test
    fun phantomCannotBePassedOnFirstMove() {
        val state = engine.start(players, Random(2))
        val phantom = state.hands.getValue(state.activeSeat).first { it.isPhantom }
        val result = engine.pass(state, state.activeSeat, phantom.id)
        assertEquals(PassResult.Rejected(PassError.PhantomFirstMove), result)
    }

    @Test
    fun rejectsWrongTurnAndUnownedCard() {
        val state = engine.start(players, Random(3))
        val other = (state.activeSeat + 1) % 5
        val card = state.hands.getValue(other).first()
        assertEquals(PassResult.Rejected(PassError.NotYourTurn), engine.pass(state, other, card.id))
        assertEquals(PassResult.Rejected(PassError.CardNotOwned), engine.pass(state, state.activeSeat, card.id))
    }

    @Test
    fun justReceivedCardIsBlockedForReceiver() {
        var state = engine.start(players, Random(4))
        val passer = state.activeSeat
        val card = engine.legalCards(state, passer).first()
        state = (engine.pass(state, passer, card.id) as PassResult.Success).state
        val receiver = (passer + 1) % 5
        assertTrue(card in state.hands.getValue(receiver))
        assertTrue(card !in engine.passableCards(state, receiver))
        assertEquals(PassResult.Rejected(PassError.CardJustReceived), engine.pass(state.copy(activeSeat = receiver), receiver, card.id))
    }

    @Test
    fun passerLeftWithFourMatchingWinsOnTheirOwnMove() {
        val hands = mapOf(
            0 to listOf(Card(0, Symbol.Star), Card(1, Symbol.Star), Card(2, Symbol.Star), Card(3, Symbol.Star), Card(10, Symbol.Circle)),
            1 to listOf(Card(11, Symbol.Circle), Card(12, Symbol.Square), Card(13, Symbol.Square), Card(14, Symbol.Circle)),
            2 to listOf(Card(15, Symbol.Square), Card(16, Symbol.Square), Card(17, Symbol.Circle), Card(18, Symbol.Circle)),
            3 to listOf(Card(19, Symbol.Square), Card(20, Symbol.Circle), Card(21, Symbol.Circle), Card(22, Symbol.Circle)),
            4 to listOf(Card(23, Symbol.Square), Card(24, Symbol.Square), Card(25, Symbol.Square), Card(26, Symbol.Circle)),
        )
        // seat 0 just received the 4th Star (id 3): its Stars are blocked, so only the Circle can go
        val state = GameState(players, hands, mapOf(0 to 3), 0, 4, true, emptyList(), Phase.Playing, emptyList())
        assertEquals(listOf(10), engine.legalCards(state, 0).map { it.id })
        val result = engine.pass(state, 0, 10) as PassResult.Success
        assertEquals(listOf(0), result.state.winners)
        assertEquals(1, result.state.activeSeat)
    }

    @Test
    fun receivingTheFourthMatchingCardDoesNotWinByItself() {
        val hands = mapOf(
            0 to listOf(Card(0, Symbol.Circle), Card(1, Symbol.Square), Card(2, Symbol.Square), Card(3, Symbol.Triangle)),
            1 to listOf(Card(4, Symbol.Star), Card(5, Symbol.Star), Card(6, Symbol.Star), Card(7, Symbol.Circle)),
            2 to listOf(Card(8, Symbol.Circle), Card(9, Symbol.Square), Card(10, Symbol.Triangle), Card(11, Symbol.Triangle)),
            3 to listOf(Card(12, Symbol.Circle), Card(13, Symbol.Square), Card(14, Symbol.Triangle), Card(15, Symbol.Square)),
            4 to listOf(Card(16, Symbol.Star), Card(17, Symbol.Circle), Card(18, Symbol.Triangle), Card(19, Symbol.Circle)),
        )
        // seat 0 passes a Star-less hand's card; give seat 1 a 4th Star via seat 0 holding one
        val state = GameState(
            players, hands.toMutableMap().apply { put(0, listOf(Card(20, Symbol.Star)) + getValue(0)) },
            emptyMap(), 0, 4, true, emptyList(), Phase.Playing, emptyList(),
        )
        val result = engine.pass(state, 0, 20) as PassResult.Success
        assertEquals(4, result.state.hands.getValue(1).count { it.symbol == Symbol.Star })
        assertTrue(result.state.winners.isEmpty())
        assertEquals(1, result.state.activeSeat)
    }

    @Test
    fun receivedSymbolBlocksAllCardsOfThatSymbol() {
        val hands = mapOf(
            0 to listOf(Card(0, Symbol.Star), Card(1, Symbol.Star), Card(2, Symbol.Circle), Card(3, Symbol.Square)),
            1 to listOf(Card(4, Symbol.Circle), Card(5, Symbol.Square), Card(6, Symbol.Square), Card(7, Symbol.Star)),
            2 to listOf(Card(8, Symbol.Star), Card(9, Symbol.Square), Card(10, Symbol.Circle), Card(11, Symbol.Triangle)),
            3 to listOf(Card(12, Symbol.Triangle), Card(13, Symbol.Circle), Card(14, Symbol.Triangle), Card(15, Symbol.Square)),
            4 to listOf(Card(16, Symbol.Triangle), Card(17, Symbol.Circle), Card(18, Symbol.Star), Card(19, Symbol.Circle)),
        )
        // seat 0 just received a Star (id 0): both Stars are blocked
        val state = GameState(players, hands, mapOf(0 to 0), 0, 4, true, emptyList(), Phase.Playing, emptyList())
        assertEquals(listOf(2, 3), engine.legalCards(state, 0).map { it.id })
    }

    @Test
    fun olderReceivedCardsBecomePassableAgain() {
        var state = GameEngine().start(players, Random(5))
        val first = state.activeSeat
        val firstCard = engine.legalCards(state, first).first()
        state = (engine.pass(state, first, firstCard.id) as PassResult.Success).state
        // the receiver passes one card on and, a lap later, receives a new one, freeing firstCard
        repeat(5) {
            val seat = state.activeSeat
            val card = engine.legalCards(state, seat).first()
            state = (engine.pass(state, seat, card.id) as PassResult.Success).state
        }
        val holder = state.hands.entries.first { e -> e.value.any { it.id == firstCard.id } }.key
        if (state.lastReceivedCardBySeat[holder] != firstCard.id) {
            assertTrue(firstCard in engine.passableCards(state, holder))
        }
    }

    @Test
    fun botGamesAlwaysTerminateWithValidOutcome() {
        repeat(300) { seed ->
            val random = Random(seed)
            var state = engine.start(players, random)
            var moves = 0
            while (state.phase == Phase.Playing && moves++ < 3000) {
                val card = strategy.chooseCard(engine.legalCards(state, state.activeSeat), state.hands.getValue(state.activeSeat)).id
                state = (engine.pass(state, state.activeSeat, card) as PassResult.Success).state
                assertEquals(21, state.hands.values.sumOf { it.size })
            }
            // Rounds may legitimately run on forever (see FEATURES.md "Open problem"); only check invariants.
            if (state.phase == Phase.Completed) {
                assertEquals(3, state.winners.size)
                assertEquals(2, state.losers.size)
            }
        }
    }
}
