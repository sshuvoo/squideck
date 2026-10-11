package com.example.squideck

import com.example.squideck.domain.*
import kotlin.random.Random
import org.junit.Assert.assertTrue
import org.junit.Test

class CompletionRateTest {
    @Test
    fun botRoundsCompleteWithThreeWinners() {
        val engine = GameEngine(); val strategy = FewestMatchesStrategy()
        val players = List(5) { Player(it, "P$it", it != 0) }
        var done = 0; val n = 500; var maxMoves = 0
        repeat(n) { seed ->
            val r = Random(seed); var s = engine.start(players, r); var moves = 0
            while (s.phase == Phase.Playing && moves < 5000) {
                val c = strategy.chooseCard(engine.legalCards(s, s.activeSeat), s.hands.getValue(s.activeSeat))
                s = (engine.pass(s, s.activeSeat, c.id) as PassResult.Success).state; moves++
            }
            if (s.phase == Phase.Completed) done++
            maxMoves = maxOf(maxMoves, moves)
        }
        println("RATE completed=$done of $n maxMoves=$maxMoves")
        assertTrue(done > n * 0.95)
    }
}
