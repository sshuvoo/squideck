# Game Architecture and Business Logic Specification

## 1. Purpose

Define the business rules, game lifecycle, player interactions, card ownership, turn progression, and win/loss conditions for a multiplayer card-matching game inspired by honeycomb-shaped cards(Squid Game Series - Dalgona).

This document is the source of truth for the game's business logic. Any implementation must follow these rules without introducing undocumented gameplay behavior.

## 2. Game Overview

The game is a multiplayer card game with exactly **5 players** per game session.

Each player receives a collection of cards featuring different symbols. Players take turns passing cards to the next player in the designated order. The objective is to collect four matching cards of an eligible symbol.

A special card, called the **Imposter** (renamed on 2026-10-12 from the original guidelines' "Phantom" / "Joker"), introduces an additional constraint to card passing. It looks like a card but matches no symbol.

The game ends when three players have secured winning positions. The remaining two players are classified as losers.

### Core game entities

- **Game:** A single multiplayer game session.
- **Player:** A participant in the game. There is no host: every player has the same role and the game manages itself.
- **Card:** A collectible game item belonging to a player.
- **Card Set:** A group of cards sharing the same eligible symbol.
- **Imposter:** A special card subject to additional passing restrictions.
- **Turn:** An opportunity for a player to pass one eligible card to the next player.
- **Winner:** A player who satisfies the winning condition and secures a winning position.

## 3. Player Roles and Responsibilities

### 3.1 No Host (changed 2026-10-12)

There is no host. All players are equal, and no player manages the game session.

- The game starts automatically as soon as the fifth player has joined; nobody presses a start button.
- Restarting after a round is not a host action (see Stage 5).

### 3.2 Players

All players participate under the same gameplay rules.

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
- The game cannot start before all five player slots are filled, and it starts by itself the moment they are.

### Stage 2: Game Initialization

Once the fifth player has joined, the game starts automatically:

1. Prepare the eligible card sets and the Imposter card.
2. Shuffle the complete card collection.
3. Distribute four regular cards to each of the five players.
4. Give the Imposter card to one randomly selected player.
5. Record the initial ownership of every card.
6. Identify the player who owns the Imposter card.
7. Designate that player as the first player to take a turn.
8. Begin the active gameplay phase.

**Distribution invariant:** Every regular card must belong to exactly one player after initialization. The Imposter must also have exactly one owner.

The stated distribution gives each player four regular cards, plus one additional Imposter card for the randomly selected recipient. Therefore, the initial distribution contains 21 cards in total if the Imposter is separate from the 20 regular cards.

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
- No additional gameplay turns occur until a new round is started.

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

### 5.2 Imposter Card

The Imposter is a special card, not an eligible matching card for the normal winning condition.

Rules governing the Imposter:

- Exactly one Imposter is assigned during a game initialization, based on the current guidelines.
- One randomly selected player receives the Imposter.
- Its initial owner starts the game.
- The Imposter cannot be passed on that player's first move.
- The Imposter must remain subject to the same ownership and turn-tracking rules as other cards, except where its special restrictions apply.

The Imposter's eligibility on later turns must be interpreted according to the confirmed passing rules.

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

The player who initially receives the Imposter takes the first turn.

After that, each player passes one eligible card to the next player in the established turn order.

The exact direction of play and how the final player passes to the first player must be explicitly defined.

### 7.2 Legal Card Pass

A card pass is valid only when all applicable conditions are satisfied:

1. It is the active player's turn.
2. The selected card currently belongs to the active player.
3. The selected card is not prohibited from being passed.
4. The active player has not previously received the selected card, according to the confirmed interpretation of the received-card rule.
5. The player satisfies any additional restrictions associated with the Imposter or another special card.

A player cannot pass a card they do not own.

### 7.3 Imposter Restriction

The initial Imposter holder starts the game by passing a card.

On the initial player's first move, the selected card must not be the Imposter.

This rule prohibits passing the Imposter itself; it does not prohibit the Imposter holder from passing an eligible regular card.

The rules must clarify whether the Imposter can be passed on later turns and what happens if the holder has no eligible card to pass.

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

The Imposter does not count as one of the four matching regular cards.

### 8.2 Winning Positions

The game has three winning positions:

1. First place
2. Second place
3. Third place

The first player to satisfy the winning condition receives first place.

The second and third players to satisfy the winning condition receive second and third place, respectively. Simultaneous wins cannot occur because only the passing player is checked after each transfer.

A player must not receive multiple winning positions within the same round.

### 8.3 Losing Condition

After three distinct players have secured winning positions, the other two players are classified as losers. The two losers are not ranked against each other.

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
2. The game cannot start before all five players have joined, and it starts automatically when the fifth joins.
3. Each player receives four regular cards during initial distribution.
4. One randomly selected player receives the additional Imposter card.
5. Each card has exactly one current owner.
6. Only the active player may perform the current turn's card-passing action.
7. A player cannot pass a card they do not own.
8. A player cannot pass a card that violates the received-card restriction.
9. The Imposter cannot be passed on the initial Imposter holder's first move.
10. Four matching eligible regular cards are required to win.
11. A player cannot claim a winning position without satisfying the winning condition.
12. A round has no more than three winning positions.
13. After three distinct winners have been recorded, the remaining two players are losers and the round is complete.
14. A completed round cannot accept additional gameplay actions.
15. Restarting creates a new round with fresh card distribution and no inherited card ownership.

## 10. Validation and Invalid Actions

The game must reject an action if it violates any applicable business rule.

Examples include:

- Starting with fewer than five players.
- Passing a card during another player's turn.
- Passing a card that the active player does not own.
- Passing a card prohibited by the received-card rule.
- Passing the Imposter on the initial holder's first move.
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
- Is the Imposter an additional twenty-first card, or is it included in the stated total of 20 cards?
- If the Imposter is additional, are the regular cards distributed uniformly across the eligible sets?

### Turn progression

- Does the next player follow a fixed clockwise or counterclockwise order?
- Does the final player pass to the first player?
- What happens if the active player has no legal card to pass?
- Can a player pass the Imposter after the initial move?

### Card history

- Does the received-card restriction apply only to the player who received a card, or does it make the card permanently unpassable?
- If a card returns to its original owner, can that player pass it again?
- Are initially distributed cards always eligible for passing, except for the Imposter restriction?

### Winning and elimination

- Is a winner immediately removed from active gameplay?
- If a winner is removed, what happens to their cards and their position in the turn order?
- Can the game continue if a player wins while a card transfer is being resolved?
- How are simultaneous wins handled?
- Must the third-place winner be recorded before the other two players are declared losers?

### Session management

- Who can start a new round after one ends, now that there is no host (currently each player's "Play again" button restarts with the same players in local mode)?
- Can players join or leave after the round starts?
- What happens if a player disconnects mid-round (there is no host to hand over to)?
- Does restarting preserve the current players or return everyone to the waiting stage?

These decisions should be confirmed before implementing the affected behavior.

## 11.1 Confirmed Decisions (2026-10-12)

Confirmed by the user and implemented in `squideck-app` (local bot mode):

- **Card composition:** 4 symbol sets × 5 copies = 20 regular cards. The 4 symbols are chosen randomly per round from the 5 eligible symbols. The Imposter is an additional 21st card.
- **Turn order:** Clockwise, wrapping from the last seat to the first. If the active player has no legal card to pass, their turn is skipped.
- **Received-card rule (revised 2026-10-12, again):** When a player receives a card, they cannot pass that card or any other card of the same symbol on their next move (e.g. after receiving a Circle, no Circle in their hand is passable). Other symbols are passable. The strict "never pass a received card" reading deadlocked ~99% of simulated rounds, so it was replaced. The Imposter is blocked on the initial holder's first move; assumption (unconfirmed): a received Imposter blocks only itself, since it has no symbol.
- **Winners:** A player wins at the end of their own move: after passing, the cards they still hold include four matching regular cards (e.g. holding four Circles plus a Star, with the Circles blocked, they pass the Star and win). Receiving the fourth matching card does not win by itself, and the player is not removed from play until they actually pass. A winner is recorded 1st/2nd/3rd, keeps their hand, is skipped in the turn order and no longer receives passes. Only the passer is checked after each transfer, so simultaneous wins cannot occur.

- **Three winners (confirmed again 2026-10-12):** The round ends as soon as three players have won; the other two lose and receive nothing. A short-lived experiment with a four-winner round was reverted at the user's request.
- **Turn timer and auto-play (2026-10-12):** Every player has a limited time per turn. If the active player does not move in time, a bot plays for them. The bot keeps playing that player's turns until the player touches the game to take control again, at which point they get a fresh full turn. Bots move after a random think time (default 1.5s–6s, always inside the limit) so they do not all move at the same pace.
- **Turn time limit:** 15 seconds (`GameConfig.turnTimeLimitMs`). The value was not specified by the user and is an assumption.
- **Timer UX:** Every seat, including the local player's at the bottom of the table, shows a countdown ring and the seconds left (teal, turning pink in the last 5 seconds). During auto-play the hint under the hand reads "A bot is playing for you. Tap a card to take over."; tapping any card hands control back.
- **Passing a card (2026-10-13):** There is no pass button. On their turn the player drags a card up out of their hand and lets go past a line, or flicks it upward, and it flies to the next player. Tapping a passable card makes it hop to teach the gesture; tapping a blocked card shakes it. Screen-reader users get a "Pass to <name>" action on each passable card.
- **Avatars (2026-10-13):** There are five anime avatars, one per seat. The player picks theirs on the name screen (default: a contestant in the pink hood), it is remembered on the device, and the bots at each table take the other four so no two players look alike. A player's avatar id is part of the player data so the server can share it.
- **Background music (2026-10-13):** A looping soundtrack plays during a round only (silent on the name, game list and lobby screens), fades in and out, plays at full volume, and drops to about half under the results card. It follows Android's guidance for game audio: Media3 ExoPlayer with game/music audio attributes, audio focus handled by the player (pauses for calls or other apps, quieter under notifications), pauses when headphones are unplugged and when the app goes to the background, releases audio focus when music stops, and the hardware volume keys control media volume. A speaker toggle in the lobby and game top bar turns music on or off, remembered on the device (default on).
  - The current track ("Pink Soldiers", Squid Game OST) is copyrighted and the repository is public, so the file is kept out of git (`squideck-app/app/src/main/assets/music/game_theme.mp3`, gitignored); builds without it are silent. A licensed track is needed before any public release.

- **Leave button (2026-10-12):** The player can leave a game from the game screen (top-left Leave button or the system Back button). During a round this asks for confirmation; after the round ends it leaves immediately. In local bot mode leaving abandons the round and returns to the game list; the entry already paid is forfeited. What leaving mid-round means for the other players in real multiplayer (bot takeover, forfeit, round cancelled) is still an open decision.
- **Entry flow (2026-10-12):** Because there is no server yet, the app simulates online play. The flow is: (1) a name screen asks for the player's name (2 to 14 characters, remembered on the device, so returning players skip it; "Change name" is on the game list); (2) the game list shows the single available table, the 10-cent table (5 players, entry 10 cents, prizes 20 / 15 / 10 cents); joining requires enough credits; (3) the lobby starts with only the player in it and the other four seats empty ("Searching for a player..."); (4) the other four players join gradually (each after a random 1.5-3.5 seconds, simulating players arriving over the internet), and each arrival plays a fast slot-machine style avatar reel scrolling upward that lands on the new player's avatar before their name fades in; (5) when the table is full the game starts by itself, about 1.5 seconds after the last avatar lands. The local player has no avatar and is shown with a badge of the first letter of their name. There is no host and no Start button. The local player sees themself as "You" in the game.
  - The wallet is shared across tables and lives outside a single table session; credits reset to 500 cents on every app launch for now.

- **Coin economy (2026-10-12, revised):**
  - Money is tracked as whole US cents and always displayed in cents everywhere in the app (credits, entry, payouts, net result), e.g. `500¢`, never as dollars. The player's money is called **credits** in the UI, never "balance". For now it is play money with a local demo wallet; no real payments.
  - Every game has a stake `S`. A player pays an entry cost equal to the stake (no extra fee on top) and must hold at least that many coins to join or start a game. Default `S` = 10 cents; the stake is one setting, any even number of cents. Starting demo credits: 500 cents.
  - Payouts: 1st `2 × S`, 2nd `1.5 × S`, 3rd `1 × S` (their stake back). The two players who do not win get `0`; they are not ranked against each other.
  - The platform keeps `0.5 × S` per game: 5 players pay `5 × S`, prizes total `4.5 × S`, the remainder is the maintenance fee.
  - Net result per player: 1st `+1 × S`, 2nd `+0.5 × S`, 3rd `0`, losers `-1 × S`. With `S` = 10 cents: entry 10, payouts 20 / 15 / 10 / 0 / 0, platform fee 5.
  - History: an earlier version charged a 10% fee on top of the stake and paid a 4th-place prize of `0.5 × S`; that was dropped when the 4th-place prize was removed, because the numbers then balance without it.
  - The coin ledger must be server-authoritative in the multiplayer API; the app only displays balances.

### Open decisions: coin economy

- The coin's name (Vermil and Kurenai were suggested; no name chosen yet), stake tiers, and how players get coins beyond the local demo wallet (top-ups, purchases).
- When the entry cost is debited (currently when the round starts) and whether it is refunded if the game never starts.
- What happens to stakes when a player leaves mid-round. Current local behaviour (assumption): the entry is already paid and is forfeited. Auto-play from the turn timer plays on normally and still earns the prize.
- What happens when a round ends in a stalemate. Current behaviour (assumption): every entry is refunded.
- Real-money play: Google Play only allows real-money gambling apps from licensed, Google-approved operators (free to download, no In-app Billing, age and region blocking). Legal review is needed before enabling real money.

### Implementation assumptions (not user-confirmed)

- Restart in local bot mode reuses the same five players; session-level restart behavior stays open for the multiplayer API.
- If no active player has a legal card, the round ends in a `Stalemate` state with no further positions awarded.

### Simulation result

With the confirmed rules above (three winners), 500 bot-only simulated rounds all finished with three winners (longest: 202 moves). The `Stalemate` state (no active player has a legal card) is kept as a safeguard but did not occur in these runs.

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
- The Imposter's behavior is fully specified.
- Winning positions and losing outcomes are determined consistently.
- Invalid actions cannot corrupt the game state.
- Restart behavior is defined.
- Every unresolved decision that affects core gameplay has been confirmed.

**Final principle:** The game should behave deterministically according to its confirmed rules, except where randomness is explicitly required, such as card shuffling and Imposter assignment.
