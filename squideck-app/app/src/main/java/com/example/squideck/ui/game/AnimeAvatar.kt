package com.example.squideck.ui.game

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.squideck.R
import kotlin.random.Random

/** The picker shows girls and boys on separate tabs. */
enum class AvatarGroup(val label: String) { Girls("Girls"), Boys("Boys") }

/** One face: a half-body anime portrait in res/drawable-nodpi. */
@Immutable
data class AvatarStyle(@param:DrawableRes val image: Int, val group: AvatarGroup)

/**
 * Every face in the game, addressed by avatar id (the index). There are more faces than seats, so when the player
 * picks theirs the bots get others and nobody at the table looks alike.
 */
object AvatarStyles {
    // Girls first so ids saved before the boys were added still point at the same face.
    val all = listOf(
        R.drawable.avatar_girl_01,
        R.drawable.avatar_girl_02,
        R.drawable.avatar_girl_03,
        R.drawable.avatar_girl_04,
        R.drawable.avatar_girl_05,
        R.drawable.avatar_girl_06,
        R.drawable.avatar_girl_07,
    ).map { AvatarStyle(it, AvatarGroup.Girls) } + listOf(
        R.drawable.avatar_boy_01,
        R.drawable.avatar_boy_02,
        R.drawable.avatar_boy_03,
        R.drawable.avatar_boy_04,
        R.drawable.avatar_boy_05,
        R.drawable.avatar_boy_06,
        R.drawable.avatar_boy_07,
    ).map { AvatarStyle(it, AvatarGroup.Boys) }

    const val DEFAULT_PLAYER_ID = 0

    fun byId(id: Int): AvatarStyle = all[id.mod(all.size)]

    fun idsIn(group: AvatarGroup): List<Int> = all.indices.filter { all[it].group == group }

    /**
     * Random faces for the bots, never the player's and never twice. They alternate girl, boy, girl, boy, girl to
     * match the bot names in seat order (Sakura, Ren, Yuki, Haruto, Mei).
     */
    fun botFaces(playerId: Int, random: Random = Random.Default): List<Int> {
        val player = playerId.mod(all.size)
        val girls = (idsIn(AvatarGroup.Girls) - player).shuffled(random)
        val boys = (idsIn(AvatarGroup.Boys) - player).shuffled(random)
        return listOf(girls[0], boys[0], girls[1], boys[1], girls[2])
    }
}

/** The portraits are half-body; avatars zoom in on the face, which sits in the upper middle of every image. */
private const val FACE_ZOOM = 1.5f
private val FaceOrigin = TransformOrigin(0.5f, 0.06f)

/** A player's face in a circle. */
@Composable
fun AnimeAvatar(style: AvatarStyle, modifier: Modifier = Modifier) {
    Image(
        painterResource(style.image),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        // Clip first so the zoomed image stays inside the circle.
        modifier = modifier.clip(CircleShape).graphicsLayer {
            scaleX = FACE_ZOOM
            scaleY = FACE_ZOOM
            transformOrigin = FaceOrigin
        },
    )
}
