package com.example.squideck.domain

import kotlin.random.Random

/** Pure game rules (see FEATURES.md). No Android or UI dependencies; all state lives in [GameState]. */
class GameEngine(
    private val config: GameConfig = GameConfig(),
    private val deckFactory: DeckFactory = DeckFactory(config),
) {
    fun start(players: List<Player>, random: Random = Random.Default): GameState {
        require(players.size == config.playerCount) { "Exactly ${config.playerCount} players are required" }
        require(players.map { it.seat }.sorted() == players.indices.toList()) { "Seats must be 0..${players.lastIndex}" }

        val deck = deckFactory.create(random)
        val phantomHolder = random.nextInt(players.size)
        val hands = players.associate { player ->
            val dealt = deck.regular.subList(player.seat * config.handSize, (player.seat + 1) * config.handSize)
            player.seat to if (player.seat == phantomHolder) dealt + deck.phantom else dealt
        }
        return GameState(
            players = players,
            hands = hands,
            lastReceivedCardBySeat = emptyMap(),
            activeSeat = phantomHolder,
            phantomHolder = phantomHolder,
            phantomHolderHasMoved = false,
            winners = emptyList(),
            phase = Phase.Playing,
            events = emptyList(),
        )
    }

    /** Cards [seat] could pass if it were their turn. */
    fun passableCards(state: GameState, seat: Int): List<Card> =
        state.hands[seat].orEmpty().filter { cardError(state, seat, it) == null }

    /** Cards the active player may pass right now. */
    fun legalCards(state: GameState, seat: Int): List<Card> =
        if (state.phase == Phase.Playing && seat == state.activeSeat) passableCards(state, seat) else emptyList()

    /** The player who would receive a card passed by [seat]. */
    fun recipientFor(state: GameState, seat: Int): Int = nextActiveSeat(state, seat, state.winners)

    fun pass(state: GameState, seat: Int, cardId: Int): PassResult {
        if (state.phase != Phase.Playing) return PassResult.Rejected(PassError.NotPlaying)
        if (seat != state.activeSeat) return PassResult.Rejected(PassError.NotYourTurn)
        val card = state.hands[seat].orEmpty().firstOrNull { it.id == cardId }
            ?: return PassResult.Rejected(PassError.CardNotOwned)
        cardError(state, seat, card)?.let { return PassResult.Rejected(it) }

        val recipient = recipientFor(state, seat)
        val hands = state.hands.toMutableMap()
        hands[seat] = hands.getValue(seat) - card
        hands[recipient] = hands.getValue(recipient) + card

        var winners = state.winners
        val events = state.events.toMutableList<GameEvent>().apply { add(GameEvent.Passed(seat, recipient, card)) }
        // A win is decided by the passer's own move: the hand left after passing must hold a full set.
        if (hasWinningSet(hands.getValue(seat))) {
            winners = winners + seat
            events += GameEvent.Won(seat, winners.size)
        }

        val moved = state.copy(
            hands = hands,
            lastReceivedCardBySeat = state.lastReceivedCardBySeat + (recipient to card.id),
            phantomHolderHasMoved = state.phantomHolderHasMoved || seat == state.phantomHolder,
            winners = winners,
            events = events,
        )
        if (winners.size == config.winnerSlots) return PassResult.Success(moved.copy(phase = Phase.Completed))
        return PassResult.Success(advanceTurn(moved, from = seat))
    }

    private fun cardError(state: GameState, seat: Int, card: Card): PassError? = when {
        isBlockedByReceived(state, seat, card) -> PassError.CardJustReceived
        card.isPhantom && seat == state.phantomHolder && !state.phantomHolderHasMoved -> PassError.PhantomFirstMove
        else -> null
    }

    /** The just-received card, and every card sharing its symbol, cannot be passed on the next move. */
    private fun isBlockedByReceived(state: GameState, seat: Int, card: Card): Boolean {
        val receivedId = state.lastReceivedCardBySeat[seat] ?: return false
        val received = state.hands[seat].orEmpty().firstOrNull { it.id == receivedId } ?: return false
        return card.id == received.id || (received.symbol != null && card.symbol == received.symbol)
    }

    private fun hasWinningSet(hand: List<Card>): Boolean =
        hand.mapNotNull { it.symbol }.groupingBy { it }.eachCount().values.any { it >= config.winSetSize }

    /** Clockwise seat after [from], skipping winners. */
    private fun nextActiveSeat(state: GameState, from: Int, winners: List<Int>): Int {
        var seat = from
        repeat(state.players.size) {
            seat = (seat + 1) % state.players.size
            if (seat !in winners) return seat
        }
        error("No active player left")
    }

    /** Hands the turn to the next player who has a legal card; players without one are skipped. */
    private fun advanceTurn(state: GameState, from: Int): GameState {
        var seat = from
        var events = state.events
        repeat(state.players.size - state.winners.size) {
            seat = nextActiveSeat(state, seat, state.winners)
            val candidate = state.copy(activeSeat = seat)
            if (passableCards(candidate, seat).isNotEmpty()) return candidate.copy(events = events)
            events = events + GameEvent.Skipped(seat)
        }
        return state.copy(phase = Phase.Stalemate, events = events)
    }
}
