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
import com.example.squideck.ui.theme.White

/** Round cross button for closing sheets and panels quickly; matches the other round icon buttons. */
@Composable
fun CloseButton(onClick: () -> Unit, modifier: Modifier = Modifier, description: String = "Close") {
    Canvas(
        modifier
            .size(GameButtonHeight)
            .clip(CircleShape)
            .background(Ink.copy(alpha = 0.35f))
            .border(1.dp, White.copy(alpha = 0.12f), CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .padding(13.dp),
    ) {
        val stroke = size.width * 0.16f
        drawLine(White, Offset.Zero, Offset(size.width, size.height), stroke, StrokeCap.Round)
        drawLine(White, Offset(size.width, 0f), Offset(0f, size.height), stroke, StrokeCap.Round)
    }
}
