package com.example.squideck.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.squideck.ui.theme.Pink
import com.example.squideck.ui.theme.PinkDark

/** The local player has no avatar, so they are shown as the first letter of their name. */
@Composable
fun InitialBadge(name: String, modifier: Modifier = Modifier, size: Dp = 56.dp) {
    Box(
        modifier.size(size).background(Brush.verticalGradient(listOf(Pink, PinkDark)), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.take(1).uppercase(),
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = (size.value * 0.42f).sp,
        )
    }
}
