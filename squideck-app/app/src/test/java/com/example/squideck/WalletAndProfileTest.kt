package com.example.squideck

import com.example.squideck.domain.economy.UsCents
import com.example.squideck.domain.economy.Wallet
import com.example.squideck.domain.profile.PlayerName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletAndProfileTest {
    @Test
    fun walletDebitsCreditsAndResets() {
        val wallet = Wallet(UsCents(500))
        assertTrue(wallet.canAfford(UsCents(10)))
        wallet.debit(UsCents(10))
        wallet.credit(UsCents(20))
        assertEquals(UsCents(510), wallet.balance.value)
        wallet.reset()
        assertEquals(UsCents(500), wallet.balance.value)
    }

    @Test(expected = IllegalArgumentException::class)
    fun walletRefusesToGoNegative() {
        Wallet(UsCents(5)).debit(UsCents(10))
    }

    @Test
    fun cannotAffordMoreThanBalance() {
        assertFalse(Wallet(UsCents(9)).canAfford(UsCents(10)))
    }

    @Test
    fun nameIsTrimmedCollapsedAndCapped() {
        assertEquals("Shuvo Dev", PlayerName.clean("  Shuvo   Dev  "))
        assertEquals(PlayerName.MAX_LENGTH, PlayerName.clean("a".repeat(40)).length)
    }

    @Test
    fun nameNeedsAtLeastTwoCharacters() {
        assertFalse(PlayerName.isValid(" a "))
        assertFalse(PlayerName.isValid("   "))
        assertTrue(PlayerName.isValid("Jo"))
    }
}
