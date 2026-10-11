package com.example.squideck

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.squideck.ui.game.GameScreen
import com.example.squideck.ui.game.GameViewModel
import com.example.squideck.ui.game.GameListScreen
import com.example.squideck.ui.game.LobbyScreen
import com.example.squideck.ui.game.NameScreen
import com.example.squideck.ui.game.Screen
import com.example.squideck.ui.theme.Background
import com.example.squideck.ui.theme.BackgroundTop
import com.example.squideck.ui.theme.Black
import com.example.squideck.ui.theme.OnDark
import com.example.squideck.ui.theme.SquideckTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            SquideckTheme {
                val viewModel: GameViewModel = viewModel(factory = GameViewModel.Factory)
                val state by viewModel.state.collectAsState()
                Scaffold(
                    modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(BackgroundTop, Background, Black))),
                    containerColor = Color.Transparent,
                    contentColor = OnDark,
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
                                state.playerName, viewModel::submitName,
                                onBack = if (state.hasName) viewModel::cancelNameChange else null,
                                modifier = content,
                            )
                            Screen.GameList -> GameListScreen(state, viewModel::joinTable, viewModel::changeName, viewModel::refillWallet, content)
                            Screen.Lobby -> if (table != null) LobbyScreen(table, viewModel::leaveTable, content)
                            Screen.Game -> if (table != null) {
                                GameScreen(
                                    table, state.selectedCardId, viewModel::selectCard, viewModel::passSelected,
                                    viewModel::restart, viewModel::takeControl, viewModel::leaveTable, content,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
