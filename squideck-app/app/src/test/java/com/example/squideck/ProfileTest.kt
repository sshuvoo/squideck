package com.example.squideck

import com.example.squideck.domain.profile.PlayerName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileTest {
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
