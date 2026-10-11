# Game Architecture and Business Logic Specification

## 1. Purpose

Define the business rules, game lifecycle, player interactions, card ownership, turn progression, and win/loss conditions for a multiplayer card-matching game inspired by honeycomb-shaped cards(Squid Game Series - Dalgona).

This document is the source of truth for the game's business logic. Any implementation must follow these rules without introducing undocumented gameplay behavior.

## 2. Game Overview

The game is a multiplayer card game with exactly **5 players** per game session.

Each player receives a collection of cards featuring different symbols. Players take turns passing cards to the next player in the designated order. The objective is to collect four matching cards of an eligible symbol.

A special card, called the **Phantom** or **Joker**, introduces an additional constraint to card passing.

The game ends when three players have secured winning positions. The remaining two players are classified as losers.

### Core game entities

- **Game:** A single multiplayer game session.
- **Host:** The player responsible for starting and restarting the game.
- **Player:** A participant in the game, including the host.
- **Card:** A collectible game item belonging to a player.
- **Card Set:** A group of cards sharing the same eligible symbol.
- **Phantom:** A special card subject to additional passing restrictions.
- **Turn:** An opportunity for a player to pass one eligible card to the next player.
- **Winner:** A player who satisfies the winning condition and secures a winning position.

## 3. Player Roles and Responsibilities

### 3.1 Host

The host is responsible for managing the game session.

- Wait until all five players have joined before starting.
- Start a new game when the player requirement is satisfied.
- Restart the game when appropriate.
- The host must not be exempt from the normal gameplay rules unless a separate rule explicitly grants an exception.

The host is a game-management role, not an automatic winning role.

### 3.2 Normal Players

Normal players participate in the game under the same gameplay rules as the host.

- Receive their initial card distribution.
- Take turns passing cards.
- Follow all card-passing restrictions.
- Attempt to collect four matching eligible cards.
- Receive a winning position or a losing outcome according to the game rules.

## 4. Game Lifecycle

A game progresses through the following conceptual stages.

### Stage 1: Waiting for Players

- The game begins in a waiting state.
- Players join the game session.
- The game requires exactly five participating players to start.
- The host cannot start the game before all five player slots are filled.

### Stage 2: Game Initialization

Once the host starts the game:

1. Prepare the eligible card sets and the Phantom card.
2. Shuffle the complete card collection.
3. Distribute four regular cards to each of the five players.
4. Give the Phantom card to one randomly selected player.
5. Record the initial ownership of every card.
6. Identify the player who owns the Phantom card.
7. Designate that player as the first player to take a turn.
8. Begin the active gameplay phase.

**Distribution invariant:** Every regular card must belong to exactly one player after initialization. The Phantom must also have exactly one owner.

The stated distribution gives each player four regular cards, plus one additional Phantom card for the randomly selected recipient. Therefore, the initial distribution contains 21 cards in total if the Phantom is separate from the 20 regular cards.

### Stage 3: Active Gameplay

- Players act in the predefined turn order.
- Each turn allows the active player to pass one eligible card to the next player.
- The recipient gains ownership of the passed card.
- A card received during gameplay becomes subject to the received-card restriction.
- Winning conditions must be evaluated whenever the game rules require a check.
- Gameplay continues until the required number of winners has been determined.

The precise behavior after a player wins must follow an explicitly confirmed rule rather than an assumed elimination mechanic.

### Stage 4: Game Completion

- The game records the first-place, second-place, and third-place winners.
- The remaining two players are classified as losers.
- The game enters a completed state.
- No additional gameplay turns occur unless the host restarts the game.

### Stage 5: Restart

A restart begins a new game round.

The new round must use a fresh shuffle and a new initial card distribution. Previous card ownership and previous-round winning positions must not affect the new round.

Whether the same five players must remain in the session for a restart is an open design decision.

## 5. Card Model and Distribution

### 5.1 Eligible Card Symbols

The current game guidelines identify the following eligible symbols:

- Star
- Square
- Triangle
- Circle
- Umbrella

Four matching cards of any eligible symbol satisfy the stated winning condition.

**Important:** The guidelines also mention four sets of similar cards. The number of card sets and the number of copies per set must be confirmed before the card collection can be defined completely.

The game must not assume that the number of sets is four or five until this inconsistency is resolved.

### 5.2 Phantom Card

The Phantom is a special card, not an eligible matching card for the normal winning condition.

Rules governing the Phantom:

- Exactly one Phantom is assigned during a game initialization, based on the current guidelines.
- One randomly selected player receives the Phantom.
- Its initial owner starts the game.
- The Phantom cannot be passed on that player's first move.
- The Phantom must remain subject to the same ownership and turn-tracking rules as other cards, except where its special restrictions apply.

The Phantom's eligibility on later turns must be interpreted according to the confirmed passing rules.

## 6. Card Ownership and History

Card ownership is central to the game's business logic.

For every card, the game must be able to determine:

- Its identity.
- Its symbol or special-card status.
- Its current owner.
- Whether it has been passed during the current round.
- Whether the active player received it through gameplay.

### 6.1 Initial Cards

Cards assigned during initial distribution are the player's starting cards.

The game must distinguish these cards from cards received through subsequent gameplay.

### 6.2 Received Cards

According to the current rules, **a player cannot pass a card they have previously received**.

This restriction applies to cards received during gameplay and must be enforced consistently across turns.

The following interpretation is proposed for clarity:

- A card received from another player becomes ineligible for passing by its new owner.
- The original owner cannot pass the card again unless the game explicitly allows the card to return and be passed under a separate rule.
- Card history must remain available for the duration of the round.

Whether a card becomes permanently unpassable after its first transfer, or only unpassable by a player who has received it, requires clarification.

## 7. Turn and Card-Passing Rules

### 7.1 Turn Order

The player who initially receives the Phantom takes the first turn.

After that, each player passes one eligible card to the next player in the established turn order.

The exact direction of play and how the final player passes to the first player must be explicitly defined.

### 7.2 Legal Card Pass

A card pass is valid only when all applicable conditions are satisfied:

1. It is the active player's turn.
2. The selected card currently belongs to the active player.
3. The selected card is not prohibited from being passed.
4. The active player has not previously received the selected card, according to the confirmed interpretation of the received-card rule.
5. The player satisfies any additional restrictions associated with the Phantom or another special card.

A player cannot pass a card they do not own.

### 7.3 Phantom Restriction

The initial Phantom holder starts the game by passing a card.

On the initial player's first move, the selected card must not be the Phantom.

This rule prohibits passing the Phantom itself; it does not prohibit the Phantom holder from passing an eligible regular card.

The rules must clarify whether the Phantom can be passed on later turns and what happens if the holder has no eligible card to pass.

### 7.4 Passing Results

When a legal pass occurs:

- The selected card leaves the sender's ownership.
- The recipient becomes the card's new owner.
- The card's transfer history is updated.
- The recipient's card collection is updated.
- The sender cannot continue to use the transferred card.
- Any applicable winning condition is evaluated according to the confirmed timing rules.

A card transfer must not produce duplicate ownership or leave a card without an owner.

## 8. Winning Conditions

### 8.1 Individual Winning Condition

A player wins when they possess **four matching regular cards from the same eligible symbol set**.

Examples of valid combinations:

- Four Stars.
- Four Squares.
- Four Triangles.
- Four Circles.
- Four Umbrellas.

A combination containing different symbols does not satisfy the matching requirement.

The Phantom does not count as one of the four matching regular cards.

### 8.2 Winning Positions

The game has three winning positions:

1. First place
2. Second place
3. Third place

The first player to satisfy the winning condition receives first place.

The second and third players to satisfy the winning condition receive second and third place, respectively, subject to confirmation of the tie-breaking rules.

A player must not receive multiple winning positions within the same round.

### 8.3 Losing Condition

After three distinct players have secured winning positions, the other two players are classified as losers.

The game is then complete.

### 8.4 Winning Evaluation

The following rules need confirmation:

- Whether the game checks for a win immediately after a card transfer or only at a specific point in the turn cycle.
- Whether a winning player is removed from active gameplay.
- Whether a winning player retains their cards and turn position.
- Whether simultaneous winning conditions are possible and how ties are resolved.
- Whether gameplay continues unchanged after a winner is recorded.

The implementation must not invent answers to these questions.

## 9. Business Invariants

These invariants must remain true throughout a game.

1. A game starts with exactly five players.
2. The host cannot start the game before all five players have joined.
3. Each player receives four regular cards during initial distribution.
4. One randomly selected player receives the additional Phantom card.
5. Each card has exactly one current owner.
6. Only the active player may perform the current turn's card-passing action.
7. A player cannot pass a card they do not own.
8. A player cannot pass a card that violates the received-card restriction.
9. The Phantom cannot be passed on the initial Phantom holder's first move.
10. Four matching eligible regular cards are required to win.
11. A player cannot claim a winning position without satisfying the winning condition.
12. A round has no more than three winning positions.
13. After three distinct winners have been recorded, the remaining two players are losers.
14. A completed round cannot accept additional gameplay actions.
15. Restarting creates a new round with fresh card distribution and no inherited card ownership.

## 10. Validation and Invalid Actions

The game must reject an action if it violates any applicable business rule.

Examples include:

- Starting with fewer than five players.
- Passing a card during another player's turn.
- Passing a card that the active player does not own.
- Passing a card prohibited by the received-card rule.
- Passing the Phantom on the initial holder's first move.
- Claiming a win with fewer than four matching eligible cards.
- Recording a second winning position for the same player.
- Continuing the round after the game has completed.
- Attempting a gameplay action before initialization or after completion.

Rejecting an invalid action must not partially apply the intended card transfer or corrupt the game state.

## 11. Decisions Requiring Clarification

The following decisions are intentionally left unresolved because the current guidelines do not fully define them.

### Card composition

- Are there four eligible symbol sets or five?
- How many regular cards belong to each set?
- Is the Phantom an additional twenty-first card, or is it included in the stated total of 20 cards?
- If the Phantom is additional, are the regular cards distributed uniformly across the eligible sets?

### Turn progression

- Does the next player follow a fixed clockwise or counterclockwise order?
- Does the final player pass to the first player?
- What happens if the active player has no legal card to pass?
- Can a player pass the Phantom after the initial move?

### Card history

- Does the received-card restriction apply only to the player who received a card, or does it make the card permanently unpassable?
- If a card returns to its original owner, can that player pass it again?
- Are initially distributed cards always eligible for passing, except for the Phantom restriction?

### Winning and elimination

- Is a winner immediately removed from active gameplay?
- If a winner is removed, what happens to their cards and their position in the turn order?
- Can the game continue if a player wins while a card transfer is being resolved?
- How are simultaneous wins handled?
- Must the third-place winner be recorded before the other two players are declared losers?

### Session management

- Can the host restart a game while a round is active?
- Can players join or leave after the round starts?
- What happens if the host disconnects?
- Does restarting preserve the current players or return everyone to the waiting stage?

These decisions should be confirmed before implementing the affected behavior.

## 11.1 Confirmed Decisions (2026-10-12)

Confirmed by the user and implemented in `squideck-app` (local bot mode):

- **Card composition:** 4 symbol sets × 5 copies = 20 regular cards. The 4 symbols are chosen randomly per round from the 5 eligible symbols. The Phantom is an additional 21st card.
- **Turn order:** Clockwise, wrapping from the last seat to the first. If the active player has no legal card to pass, their turn is skipped.
- **Received-card rule (revised 2026-10-12, again):** When a player receives a card, they cannot pass that card or any other card of the same symbol on their next move (e.g. after receiving a Circle, no Circle in their hand is passable). Other symbols are passable. The strict "never pass a received card" reading deadlocked ~99% of simulated rounds, so it was replaced. The Phantom is blocked on the initial holder's first move; assumption (unconfirmed): a received Phantom blocks only itself, since it has no symbol.
- **Winners:** A player wins at the end of their own move: after passing, the cards they still hold include four matching regular cards (e.g. holding four Circles plus a Star, with the Circles blocked, they pass the Star and win). Receiving the fourth matching card does not win by itself, and the player is not removed from play until they actually pass. A winner is recorded 1st/2nd/3rd, keeps their hand, is skipped in the turn order and no longer receives passes. Only the passer is checked after each transfer, so simultaneous wins cannot occur.

### Implementation assumptions (not user-confirmed)

- Restart in local bot mode reuses the same five players; session-level restart behavior stays open for the multiplayer API.
- If no active player has a legal card, the round ends in a `Stalemate` state with no further positions awarded.

### Simulation result

With the confirmed rules above, 500 bot-only simulated rounds all finished with three winners (longest: 144 moves). The `Stalemate` state (no active player has a legal card) is kept as a safeguard but did not occur in these runs.

## 12. Agent Implementation Directives

When implementing this game's business logic:

- Treat this document as the authoritative description of the confirmed rules.
- Keep business decisions independent of presentation and infrastructure choices.
- Do not introduce additional gameplay rules without authorization.
- Do not infer missing behavior when it affects card ownership, turn order, fairness, or winning outcomes.
- If requirements conflict with one another or with a new user prompt, notify the user and ask for clarification.
- Update this specification only after the user confirms how a conflict should be resolved.
- Preserve all established business invariants when changing individual rules.
- When a rule changes, identify its impact on related rules and request clarification if the change creates a conflict.
- Keep unresolved decisions explicit until the user confirms them.
- Validate every state-changing gameplay action against the current game state and applicable business rules.

## 13. Definition of Completion

The business logic is considered complete when:

- A valid five-player game can be initialized.
- The card collection is defined unambiguously.
- Cards are distributed and ownership is tracked correctly.
- Turn order and card-passing restrictions are enforced.
- The Phantom's behavior is fully specified.
- Winning positions and losing outcomes are determined consistently.
- Invalid actions cannot corrupt the game state.
- Restart behavior is defined.
- Every unresolved decision that affects core gameplay has been confirmed.

**Final principle:** The game should behave deterministically according to its confirmed rules, except where randomness is explicitly required, such as card shuffling and Phantom assignment.
