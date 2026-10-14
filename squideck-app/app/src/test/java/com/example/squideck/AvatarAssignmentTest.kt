package com.example.squideck

import com.example.squideck.ui.game.AvatarGroup
import com.example.squideck.ui.game.AvatarStyles
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AvatarAssignmentTest {
    @Test
    fun thereAreSevenGirlsAndSevenBoys() {
        assertEquals(7, AvatarStyles.idsIn(AvatarGroup.Girls).size)
        assertEquals(7, AvatarStyles.idsIn(AvatarGroup.Boys).size)
    }

    @Test
    fun botFacesAreDistinctNeverThePlayersAndAlternateGirlBoy() {
        AvatarStyles.all.indices.forEach { playerId ->
            val bots = AvatarStyles.botFaces(playerId, Random(playerId))
            assertEquals(5, bots.toSet().size)
            assertFalse(playerId in bots)
            assertEquals(
                listOf(AvatarGroup.Girls, AvatarGroup.Boys, AvatarGroup.Girls, AvatarGroup.Boys, AvatarGroup.Girls),
                bots.map { AvatarStyles.byId(it).group },
            )
        }
    }
}
