package com.example.squideck.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.squideck.domain.economy.UsCents
import com.example.squideck.ui.theme.Ink
import com.example.squideck.ui.theme.White

/** The player's credits, shown the same way on every screen. */
@Composable
fun CreditsChip(credits: UsCents, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier
            .clip(shape)
            .background(Ink.copy(alpha = 0.35f))
            .border(1.dp, White.copy(alpha = 0.12f), shape)
            // Same height as the buttons beside it (music toggle, info), so the top bar lines up.
            .height(GameButtonHeight)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GameText(
            "CREDITS",
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        GameText(credits.toString(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
    }
}
