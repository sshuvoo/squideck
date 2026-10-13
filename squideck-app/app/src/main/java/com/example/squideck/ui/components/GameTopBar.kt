package com.example.squideck.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.squideck.domain.economy.UsCents

/** Leave on the left, music toggle and credits on the right: the same bar in the lobby and at the table. */
@Composable
fun GameTopBar(
    onLeave: () -> Unit,
    credits: UsCents,
    musicEnabled: Boolean,
    onToggleMusic: () -> Unit,
    modifier: Modifier = Modifier,
    freePlay: Boolean = false,
) {
    Row(
        modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GameButton("Leave", onLeave, style = GameButtonStyle.Ghost, size = GameButtonSize.Small, leading = "✕")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            MusicToggle(musicEnabled, onToggleMusic)
            CreditsChip(credits, freePlay = freePlay)
        }
    }
}
