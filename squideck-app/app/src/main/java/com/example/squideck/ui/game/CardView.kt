package com.example.squideck.ui.game

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.squideck.domain.Card
import com.example.squideck.ui.components.GameText
import com.example.squideck.ui.theme.Ink
import com.example.squideck.ui.theme.Pink
import com.example.squideck.ui.theme.White

val CardBaseWidth = 76.dp
val CardBaseHeight = 108.dp

@Composable
fun CardView(
    card: Card,
    selected: Boolean,
    received: Boolean,
    blocked: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = CardBaseWidth,
) {
    val height = width * (CardBaseHeight / CardBaseWidth)
    val compact = width < CardBaseWidth
    val lift by animateDpAsState(if (selected) (-14).dp else 0.dp, label = "lift")
    Surface(
        modifier = modifier
            .offset(y = lift)
            .size(width = width, height = height)
            .alpha(if (enabled || selected) 1f else 0.55f)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = buildString {
                    append(if (card.isImposter) "Imposter" else card.symbol!!.name)
                    append(" card")
                    if (received) append(", just received")
                    if (blocked) append(", blocked this turn")
                    if (selected) append(", selected")
                }
            },
        shape = RoundedCornerShape(14.dp),
        color = Ink,
        border = BorderStroke(if (selected) 3.dp else 1.5.dp, if (selected) Pink else White.copy(alpha = 0.3f)),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SymbolIcon(card.symbol, Modifier.size(width * 0.58f))
            GameText(
                text = when {
                    card.isImposter -> "Imposter"
                    received -> "Received"
                    blocked -> "Blocked"
                    else -> card.symbol!!.name
                },
                style = MaterialTheme.typography.labelSmall,
                fontSize = if (compact) 9.sp else MaterialTheme.typography.labelSmall.fontSize,
                maxLines = 1,
                color = if (received || blocked) Pink else White.copy(alpha = 0.75f),
            )
        }
    }
}
