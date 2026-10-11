package com.example.squideck

import com.example.squideck.domain.GameState
import com.example.squideck.domain.Phase
import com.example.squideck.domain.Player
import com.example.squideck.domain.economy.Economy
import com.example.squideck.domain.economy.EconomyConfig
import com.example.squideck.domain.economy.UsCents
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class EconomyTest {
    private val players = List(5) { Player(it, "P$it", it == 0, it != 0) }
    private val config = EconomyConfig()

    private fun game(phase: Phase, winners: List<Int>) =
        GameState(players, players.associate { it.seat to emptyList() }, emptyMap(), 0, 0, true, winners, phase, emptyList())

    @Test
    fun entryEqualsStake() {
        assertEquals(UsCents(10), config.stake)
        assertEquals(UsCents(10), config.entryCost)
    }

    @Test
    fun prizeTableMatchesSpec() {
        assertEquals(listOf(20L, 15L, 10L, 0L, 0L), (1..5).map { config.payoutFor(it).value })
    }

    @Test
    fun poolBalancesAndPlatformKeepsHalfStake() {
        val collected = config.entryCost * 5
        val paid = (1..5).fold(UsCents(0)) { total, place -> total + config.payoutFor(place) }
        assertEquals(UsCents(5), config.platformFee(5))
        assertEquals(collected, paid + config.platformFee(5))
    }

    @Test
    fun otherStakesScaleWithoutRounding() {
        val big = EconomyConfig(stake = UsCents(100))
        assertEquals(UsCents(100), big.entryCost)
        assertEquals(UsCents(50), big.platformFee(5))
        assertEquals(listOf(200L, 150L, 100L, 0L, 0L), (1..5).map { big.payoutFor(it).value })
    }

    @Test(expected = IllegalArgumentException::class)
    fun stakeMustBeEven() {
        EconomyConfig(stake = UsCents(15))
    }

    @Test
    fun settlementPaysTopThreeAndNothingToLosers() {
        val settlement = Economy.settle(game(Phase.Completed, winners = listOf(3, 1, 4)), config)
        assertNotNull(settlement)
        assertEquals(listOf(3, 1, 4, 0, 2), settlement!!.places)
        assertEquals(UsCents(20), settlement.payoutFor(3))
        assertEquals(UsCents(15), settlement.payoutFor(1))
        assertEquals(UsCents(10), settlement.payoutFor(4))
        assertEquals(UsCents(0), settlement.payoutFor(0))
        assertEquals(UsCents(0), settlement.payoutFor(2))
        assertEquals(UsCents(10), settlement.netFor(3))
        assertEquals(UsCents(0), settlement.netFor(4))
        assertEquals(UsCents(-10), settlement.netFor(2))
    }

    @Test
    fun runningRoundHasNoSettlementAndStalemateRefunds() {
        assertNull(Economy.settle(game(Phase.Playing, emptyList()), config))
        val refund = Economy.settle(game(Phase.Stalemate, listOf(1)), config)!!
        assertEquals(true, refund.refunded)
        assertEquals(UsCents(0), refund.netFor(0))
    }

    @Test
    fun moneyFormatsAsDollars() {
        assertEquals("$0.05", UsCents(5).toString())
        assertEquals("$1.10", UsCents(110).toString())
        assertEquals("-$0.11", UsCents(-11).toString())
        assertEquals("+$0.09", UsCents(9).toSignedString())
    }
}
