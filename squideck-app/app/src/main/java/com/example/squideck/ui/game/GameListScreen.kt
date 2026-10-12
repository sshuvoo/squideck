package com.example.squideck.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.example.squideck.domain.GameConfig
import com.example.squideck.domain.economy.UsCents
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
    modifier: Modifier = Modifier,
) {
    var showRules by rememberSaveable { mutableStateOf(false) }
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
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
            Box {
                // Faint brand mark in the corner gives the card some character.
                BrandSymbols(40.dp, Modifier.align(Alignment.TopEnd).padding(18.dp), color = Pink.copy(alpha = 0.12f))
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(8.dp).background(Teal, CircleShape))
                        GameText("OPEN", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    }
                    GameText("${economy.entryCost} table", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    GameText("5 players · 3 winners · entry ${economy.entryCost}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PrizeChip("1ST", economy.payoutFor(1), highlight = true, modifier = Modifier.weight(1f))
                        PrizeChip("2ND", economy.payoutFor(2), modifier = Modifier.weight(1f))
                        PrizeChip("3RD", economy.payoutFor(3), modifier = Modifier.weight(1f))
                    }
                    GameButton("Join · ${economy.entryCost}", onJoin, Modifier.fillMaxWidth(), enabled = state.canAffordEntry)
                    if (!state.canAffordEntry) {
                        GameText("Not enough credits to play.", color = Pink)
                        GameButton("Add demo credits", onRefillWallet, style = GameButtonStyle.Secondary, size = GameButtonSize.Small)
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        GameButton("How to play", { showRules = true }, Modifier.fillMaxWidth(), style = GameButtonStyle.Secondary)
    }
    if (showRules) {
        HowToPlaySheet(economy, GameConfig().turnTimeLimitMs / 1000, onDismiss = { showRules = false })
    }
}

@Composable
private fun PrizeChip(place: String, amount: UsCents, modifier: Modifier = Modifier, highlight: Boolean = false) {
    Column(
        modifier
            .background(Ink.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GameText(place, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        GameText(amount.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = if (highlight) Pink else White)
    }
}
