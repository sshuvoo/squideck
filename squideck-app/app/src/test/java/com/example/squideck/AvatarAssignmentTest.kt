package com.example.squideck

import com.example.squideck.ui.game.AvatarStyles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AvatarAssignmentTest {
    @Test
    fun thereIsOneFacePerSeat() {
        assertEquals(5, AvatarStyles.all.size)
    }

    @Test
    fun botsGetEveryFaceExceptThePlayers() {
        AvatarStyles.all.indices.forEach { playerId ->
            val bots = AvatarStyles.idsExcept(playerId)
            assertEquals(4, bots.toSet().size)
            assertFalse(playerId in bots)
        }
    }
}
