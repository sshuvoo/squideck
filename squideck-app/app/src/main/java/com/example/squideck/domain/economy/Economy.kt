package com.example.squideck.domain.economy

import com.example.squideck.domain.GameState
import com.example.squideck.domain.Phase

/**
 * Stake and prize table (see FEATURES.md "Coin economy"). Entry equals the stake; the platform keeps
 * whatever the prizes do not pay out. Payouts are expressed in halves of the stake (4 = 2x, 3 = 1.5x, ...).
 */
data class EconomyConfig(
    val stake: UsCents = UsCents(10),
    val payoutHalves: List<Int> = listOf(4, 3, 2, 0, 0),
) {
    init {
        require(stake.value > 0 && stake.value % 2 == 0L) { "Stake must be a positive even number of cents so half-stake prizes are whole" }
        require(payoutHalves.isNotEmpty()) { "A prize table is required" }
    }

    val entryCost: UsCents = stake

    /** Prize for a finishing position; positions beyond the table (the losers) get nothing. */
    fun payoutFor(place: Int): UsCents = UsCents(stake.value * payoutHalves.getOrElse(place - 1) { 0 } / 2)

    /** What the platform keeps from a full game: everything collected minus every prize. */
    fun platformFee(playerCount: Int): UsCents =
        entryCost * playerCount - (1..playerCount).fold(UsCents(0)) { total, place -> total + payoutFor(place) }
}

/** Who got what when a round finished. */
data class Settlement(
    /** Winners in finishing order (index 0 is first place), then the players who lost. For a cancelled round the order is not a ranking. */
    val places: List<Int>,
    val payouts: Map<Int, UsCents>,
    val entryCost: UsCents,
    val platformFee: UsCents,
    /** True when the round was cancelled and every entry was refunded. */
    val refunded: Boolean,
) {
    fun payoutFor(seat: Int): UsCents = payouts.getValue(seat)
    fun netFor(seat: Int): UsCents = payoutFor(seat) - entryCost
}

object Economy {
    /** Places only, no money: for free-play (nearby) rounds. */
    fun settleFree(game: GameState): Settlement? {
        val seats = game.players.map { it.seat }
        if (game.phase == Phase.Playing) return null
        return Settlement(
            places = game.winners + (seats - game.winners.toSet()),
            payouts = seats.associateWith { UsCents(0) },
            entryCost = UsCents(0),
            platformFee = UsCents(0),
            refunded = game.phase == Phase.Stalemate,
        )
    }

    /** Settles a finished round; returns null while it is still running. */
    fun settle(game: GameState, config: EconomyConfig): Settlement? {
        val seats = game.players.map { it.seat }
        return when (game.phase) {
            Phase.Playing -> null
            Phase.Completed -> {
                val places = game.winners + (seats - game.winners.toSet())
                Settlement(
                    places = places,
                    payouts = places.mapIndexed { index, seat -> seat to config.payoutFor(index + 1) }.toMap(),
                    entryCost = config.entryCost,
                    platformFee = config.platformFee(seats.size),
                    refunded = false,
                )
            }
            // Open decision: a stalemate has no ranking, so entries are refunded in full for now.
            Phase.Stalemate -> Settlement(
                places = game.winners + (seats - game.winners.toSet()),
                payouts = seats.associateWith { config.entryCost },
                entryCost = config.entryCost,
                platformFee = UsCents(0),
                refunded = true,
            )
        }
    }
}
