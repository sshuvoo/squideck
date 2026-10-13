package com.example.squideck.net

import com.example.squideck.domain.GameEvent
import com.example.squideck.domain.TurnTimer
import com.example.squideck.domain.round.RoundState

/** Packs a round for sending: timers as time remaining, and only the tail of the event log. */
fun RoundState.toSnapshot(nowMs: Long): RoundSnapshot = RoundSnapshot(
    game = game.copy(events = game.events.takeLast(SNAPSHOT_EVENT_TAIL)),
    eventsTotal = game.events.size,
    turnSeat = turnTimer?.seat,
    turnRemainingMs = turnTimer?.remainingMs(nowMs) ?: 0,
    turnDurationMs = turnTimer?.durationMs ?: 0,
    countdownRemainingMs = countdown?.remainingMs(nowMs),
    countdownDurationMs = countdown?.durationMs ?: 0,
    autoPlaySeats = autoPlaySeats,
)

/** Rebuilds a round on a receiving phone, restarting the timers on its own clock. */
fun RoundSnapshot.toRoundState(knownEvents: List<GameEvent>, nowMs: Long): RoundState = RoundState(
    game = game.copy(events = mergeEvents(knownEvents, this)),
    turnTimer = turnSeat?.let { TurnTimer(it, nowMs - (turnDurationMs - turnRemainingMs), turnDurationMs) },
    countdown = countdownRemainingMs?.let { TurnTimer(-1, nowMs - (countdownDurationMs - it), countdownDurationMs) },
    autoPlaySeats = autoPlaySeats,
)
