package com.example.squideck.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.squideck.ui.theme.Ink
import com.example.squideck.ui.theme.Pink
import com.example.squideck.ui.theme.White

/** One choice in [ModeTabs]; [badge] marks it, e.g. "SOON" for a mode that isn't available yet. */
data class ModeTab(val label: String, val enabled: Boolean = true, val badge: String? = null)

/** Segmented picker for how to play (online, nearby, bots); the chosen segment glows pink. */
@Composable
fun ModeTabs(tabs: List<ModeTab>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Ink.copy(alpha = 0.35f))
            .border(1.dp, White.copy(alpha = 0.1f), shape)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tabs.forEachIndexed { index, tab ->
            val chosen = index == selected
            val fill by animateColorAsState(if (chosen) Pink else Ink.copy(alpha = 0f), label = "tab$index")
            Box(
                Modifier
                    .weight(1f)
                    .height(GameButtonHeight)
                    .clip(RoundedCornerShape(10.dp))
                    .background(fill)
                    .selectable(selected = chosen, enabled = tab.enabled, role = Role.Tab) { onSelect(index) }
                    .alpha(if (tab.enabled) 1f else 0.45f),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    GameText(tab.label, fontWeight = FontWeight.Black, letterSpacing = 1.sp, style = MaterialTheme.typography.labelLarge)
                    if (tab.badge != null) {
                        GameText(tab.badge, style = MaterialTheme.typography.labelSmall, fontSize = 8.sp, letterSpacing = 1.sp, color = White.copy(alpha = 0.8f))
                    }
                }
            }
        }
    }
}
