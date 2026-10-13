package com.example.squideck

import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.squideck.audio.GameMusic
import com.example.squideck.ui.components.ConfirmDialog
import com.example.squideck.ui.game.GameListScreen
import com.example.squideck.ui.game.GameScreen
import com.example.squideck.ui.game.GameViewModel
import com.example.squideck.ui.game.LobbyScreen
import com.example.squideck.ui.game.NameScreen
import com.example.squideck.ui.game.Screen
import com.example.squideck.ui.intro.IntroScreen
import com.example.squideck.ui.theme.DarkTeal
import com.example.squideck.ui.theme.Ink
import com.example.squideck.ui.theme.SquideckTheme
import com.example.squideck.ui.theme.White

class MainActivity : ComponentActivity() {
    private lateinit var music: GameMusic

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        music = GameMusic(this).also { lifecycle.addObserver(it) }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            SquideckTheme {
                val viewModel: GameViewModel = viewModel(factory = GameViewModel.Factory)
                val state by viewModel.state.collectAsState()
                // The intro plays once per launch; rememberSaveable keeps it from replaying on rotation.
                var showIntro by rememberSaveable { mutableStateOf(true) }
                if (showIntro) {
                    IntroScreen(onFinished = { showIntro = false })
                    return@SquideckTheme
                }
                LaunchedEffect(state.musicPlaying, state.roundOver) {
                    music.update(play = state.musicPlaying, duck = state.roundOver)
                }
                Scaffold(
                    // Ink base with a dark-teal glow from the top, like the guards' elevator.
                    modifier = Modifier.fillMaxSize().background(Ink).background(Brush.verticalGradient(listOf(DarkTeal.copy(alpha = 0.45f), DarkTeal.copy(alpha = 0f)))),
                    containerColor = Color.Transparent,
                    contentColor = White,
                ) { innerPadding ->
                    val content = Modifier.padding(innerPadding)
                    val table = state.session
                    // Screens cross-fade with a slight rise so moving between them feels like one app, not page loads.
                    AnimatedContent(
                        targetState = state.screen,
                        transitionSpec = {
                            (fadeIn(tween(280)) + slideInVertically(tween(280)) { it / 16 }) togetherWith fadeOut(tween(180))
                        },
                        label = "screen",
                    ) { screen ->
                        when (screen) {
                            Screen.Name -> NameScreen(
                                state.playerName, state.avatarId, viewModel::submitProfile,
                                onBack = if (state.hasName) viewModel::cancelNameChange else null,
                                modifier = content,
                            )
                            Screen.GameList -> GameListScreen(
                                state, viewModel::joinTable,
                                viewModel::startNearbySearch, viewModel::stopNearbySearch, viewModel::createRoom, viewModel::joinRoom,
                                viewModel::changeName,
                                // A clean exit: finish the activity, which also releases the music player.
                                onExit = { finish() },
                                modifier = content,
                            )
                            Screen.Lobby -> if (table != null) {
                                LobbyScreen(table, state.musicEnabled, viewModel::toggleMusic, viewModel::leaveTable, viewModel::startRoomNow, content)
                            }
                            Screen.Game -> if (table != null) {
                                GameScreen(
                                    table, viewModel::passCard, viewModel::tapCard, viewModel::restart, viewModel::leaveTable,
                                    state.musicEnabled, viewModel::toggleMusic, content,
                                )
                            }
                        }
                    }
                    // A nearby room can end on its own (closed by its creator, connection lost); say why, then leave.
                    table?.endedReason?.let { reason ->
                        ConfirmDialog(
                            title = "Room ended",
                            message = reason,
                            confirmLabel = "OK",
                            dismissLabel = null,
                            onConfirm = viewModel::leaveTable,
                            onDismiss = viewModel::leaveTable,
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Volume keys change the game's media volume even between songs (Android guidance for games).
        volumeControlStream = AudioManager.STREAM_MUSIC
    }
}
