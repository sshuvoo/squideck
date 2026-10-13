package com.example.squideck.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.squideck.domain.economy.UsCents
import com.example.squideck.ui.components.ConfirmDialog
import com.example.squideck.ui.components.CreditsChip
import com.example.squideck.ui.components.GameButton
import com.example.squideck.ui.components.GameButtonSize
import com.example.squideck.ui.components.GameButtonStyle
import com.example.squideck.ui.components.GameText
import com.example.squideck.ui.theme.Ink
import com.example.squideck.ui.theme.Pink
import com.example.squideck.ui.theme.Teal
import com.example.squideck.ui.theme.White

/** Lists the tables you can sit at. For now there is a single 10-cent table. */
@Composable
fun GameListScreen(
    state: GameUiState,
    onJoin: () -> Unit,
    onChangeName: () -> Unit,
    onRefillWallet: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showRules by rememberSaveable { mutableStateOf(false) }
    var confirmExit by rememberSaveable { mutableStateOf(false) }
    // The game list is the home screen: Back asks before closing the app instead of just minimising it.
    BackHandler { confirmExit = true }
    if (confirmExit) {
        ConfirmDialog(
            title = "Exit game?",
            message = "Do you want to close Squideck?",
            confirmLabel = "Exit",
            dismissLabel = "Stay",
            onConfirm = onExit,
            onDismiss = { confirmExit = false },
        )
    }
    val economy = state.economy
    Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            BrandMark()
            CreditsChip(state.credits)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AnimeAvatar(AvatarStyles.byId(state.avatarId), Modifier.size(48.dp))
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                GameText("Hi, ${state.playerName}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                GameText("Ready for a round?", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            GameButton("Edit", onChangeName, style = GameButtonStyle.Ghost, size = GameButtonSize.Small)
        }
        GameText(
            "TABLES",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // The table is the show's invitation card. Tapping it always opens the details and rules first; the
        // sheet's bottom button joins.
        InvitationCard(
            footer = if (state.canAffordEntry) "Tap to join · ${economy.entryCost}" else "Not enough credits",
            onClick = { showRules = true },
            clickDescription = "Open the ${economy.entryCost} table",
        )
        if (!state.canAffordEntry) {
            GameButton("Add demo credits", onRefillWallet, style = GameButtonStyle.Secondary, size = GameButtonSize.Small)
        }
    }
    if (showRules) {
        RulesSheet(
            squideckRules(economy),
            onDismiss = { showRules = false },
            actionLabel = if (state.canAffordEntry) "Join · ${economy.entryCost}" else "Not enough credits",
            actionEnabled = state.canAffordEntry,
            onAction = {
                showRules = false
                onJoin()
            },
        )
    }
}
