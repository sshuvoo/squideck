package com.example.squideck.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.squideck.ui.theme.Ink
import com.example.squideck.ui.theme.Teal
import com.example.squideck.ui.theme.White

/** Round "i" button that opens a game's rules; matches the music toggle and credits chip. */
@Composable
fun InfoButton(onClick: () -> Unit, description: String, modifier: Modifier = Modifier) {
    Canvas(
        modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Ink.copy(alpha = 0.35f))
            .border(1.5.dp, Teal.copy(alpha = 0.7f), CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .padding(12.dp),
    ) {
        val w = size.width
        val h = size.height
        drawCircle(White, radius = w * 0.11f, center = Offset(w / 2, h * 0.14f))
        drawLine(White, Offset(w / 2, h * 0.42f), Offset(w / 2, h * 0.9f), strokeWidth = w * 0.2f, cap = StrokeCap.Round)
    }
}
