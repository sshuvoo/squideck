package com.example.squideck.audio

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Looping background music for a round, following Android's guidance for game audio:
 * - Media3 ExoPlayer with game/music audio attributes, and audio focus handled by the player, so music pauses
 *   for calls or other apps and ducks under notifications (Android 12+ enforces this for games).
 * - Pauses when headphones are unplugged ("becoming noisy") and whenever the app leaves the screen.
 * - Fades in and out instead of cutting, and plays below full volume so it never drowns out the game.
 *
 * The track is an asset kept out of version control (see squideck-app/.gitignore); if it is missing the
 * game simply stays silent.
 */
class GameMusic(context: Context, private val track: Uri = DEFAULT_TRACK) : DefaultLifecycleObserver {
    private val scope = MainScope()
    private var fade: Job? = null
    private var prepared = false
    private var available = true
    private var inForeground = false
    private var wanted = false
    private var ducked = false

    private val player: ExoPlayer = ExoPlayer.Builder(context.applicationContext)
        .setAudioAttributes(
            AudioAttributes.Builder().setUsage(C.USAGE_GAME).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
            /* handleAudioFocus = */ true,
        )
        .setHandleAudioBecomingNoisy(true)
        .build()
        .apply {
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    available = false
                }
            })
        }

    /** What the game wants: music during a round ([play]), quieter under the results card ([duck]). */
    fun update(play: Boolean, duck: Boolean) {
        wanted = play
        ducked = duck
        apply()
    }

    override fun onStart(owner: LifecycleOwner) {
        inForeground = true
        apply()
    }

    override fun onStop(owner: LifecycleOwner) {
        inForeground = false
        fade?.cancel()
        player.pause()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        scope.cancel()
        player.release()
    }

    private fun apply() {
        if (!available) return
        when {
            wanted && inForeground -> {
                // Each round starts the track from the top.
                if (!prepared) {
                    player.setMediaItem(MediaItem.fromUri(track))
                    player.prepare()
                    prepared = true
                }
                player.play()
                fadeTo(if (ducked) DUCKED_VOLUME else PLAY_VOLUME)
            }
            // Music is over (left the table or turned off): stop fully, which also gives audio focus back so the
            // player's own music apps can resume.
            !wanted && prepared -> fadeTo(0f) {
                player.stop()
                prepared = false
            }
            // A short break such as the app going to the background keeps focus and position.
            else -> player.pause()
        }
    }

    /** Clock-based, so the fade takes [FADE_MS] even when the main thread is busy and frames arrive late. */
    private fun fadeTo(target: Float, then: () -> Unit = {}) {
        fade?.cancel()
        fade = scope.launch {
            val start = player.volume
            val began = SystemClock.uptimeMillis()
            while (true) {
                val progress = ((SystemClock.uptimeMillis() - began).toFloat() / FADE_MS).coerceAtMost(1f)
                player.volume = start + (target - start) * progress
                if (progress >= 1f) break
                delay(FRAME_MS)
            }
            then()
        }
    }

    private companion object {
        val DEFAULT_TRACK: Uri = Uri.parse("asset:///music/game_theme.mp3")
        const val PLAY_VOLUME = 0.6f
        const val DUCKED_VOLUME = 0.25f
        const val FADE_MS = 700L
        const val FRAME_MS = 16L
    }
}
