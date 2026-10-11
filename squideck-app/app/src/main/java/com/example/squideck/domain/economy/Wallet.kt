package com.example.squideck.domain.economy

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * The player's credits, shared by every screen and table. Local demo wallet for now; once the server
 * exists the ledger lives there and this becomes a read-only view of it.
 */
class Wallet(private val startingCredits: UsCents = UsCents(500)) {
    private val _balance = MutableStateFlow(startingCredits)
    val balance: StateFlow<UsCents> = _balance.asStateFlow()

    fun canAfford(amount: UsCents): Boolean = _balance.value >= amount

    fun debit(amount: UsCents) {
        require(canAfford(amount)) { "Not enough credits" }
        _balance.update { it - amount }
    }

    fun credit(amount: UsCents) {
        _balance.update { it + amount }
    }

    /** Demo only: back to the starting credits. */
    fun reset() {
        _balance.value = startingCredits
    }
}
