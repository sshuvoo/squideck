package com.example.squideck.domain.session

import com.example.squideck.domain.GameConfig
import com.example.squideck.domain.GameEngine
import com.example.squideck.domain.Phase
import com.example.squideck.domain.Player
import com.example.squideck.domain.economy.Economy
import com.example.squideck.domain.economy.EconomyConfig
import com.example.squideck.domain.economy.UsCents
import com.example.squideck.domain.round.RoundState

/** Builds what the player in [localSeat] sees: their legal cards, who receives their pass, timers and results. */
fun sessionView(
    players: List<Player>,
    round: RoundState?,
    localSeat: Int,
    engine: GameEngine,
    config: GameConfig,
    balance: UsCents,
    economy: EconomyConfig,
    freePlay: Boolean = false,
    room: RoomInfo? = null,
): SessionState {
    val game = round?.game
    val countingDown = round?.countdown != null
    return SessionState(
        players = players,
        game = game,
        legalCardIds = if (game != null && !countingDown) engine.legalCards(game, localSeat).map { it.id }.toSet() else emptySet(),
        recipientSeat = if (game != null && game.phase == Phase.Playing) engine.recipientFor(game, localSeat) else null,
        turnTimer = round?.turnTimer,
        countdown = round?.countdown,
        autoPlay = round != null && localSeat in round.autoPlaySeats,
        localSeat = localSeat,
        playerCount = config.playerCount,
        balance = balance,
        economy = economy,
        settlement = game?.let { if (freePlay) Economy.settleFree(it) else Economy.settle(it, economy) },
        freePlay = freePlay,
        room = room,
    )
}
