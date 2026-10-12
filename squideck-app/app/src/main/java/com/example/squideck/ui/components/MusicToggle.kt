package com.example.squideck.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.squideck.ui.theme.Ink
import com.example.squideck.ui.theme.Pink
import com.example.squideck.ui.theme.White

/** Round speaker button that turns the game music on and off; matches the credits chip. */
@Composable
fun MusicToggle(enabled: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Canvas(
        modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Ink.copy(alpha = 0.35f))
            .border(1.dp, White.copy(alpha = 0.12f), CircleShape)
            .toggleable(value = enabled, role = Role.Switch, onValueChange = { onToggle() })
            .semantics { contentDescription = "Music" }
            .padding(10.dp),
    ) {
        val w = size.width
        val h = size.height
        val color = if (enabled) White else White.copy(alpha = 0.5f)
        val speaker = Path().apply {
            moveTo(w * 0.05f, h * 0.36f)
            lineTo(w * 0.28f, h * 0.36f)
            lineTo(w * 0.55f, h * 0.1f)
            lineTo(w * 0.55f, h * 0.9f)
            lineTo(w * 0.28f, h * 0.64f)
            lineTo(w * 0.05f, h * 0.64f)
            close()
        }
        drawPath(speaker, color)
        val stroke = Stroke(w * 0.09f, cap = StrokeCap.Round)
        if (enabled) {
            drawArc(color, -50f, 100f, false, Offset(w * 0.42f, h * 0.3f), Size(w * 0.4f, h * 0.4f), style = stroke)
            drawArc(color, -55f, 110f, false, Offset(w * 0.34f, h * 0.12f), Size(w * 0.66f, h * 0.76f), style = stroke)
        } else {
            drawLine(Pink, Offset(w * 0.7f, h * 0.34f), Offset(w * 0.98f, h * 0.66f), w * 0.1f, StrokeCap.Round)
            drawLine(Pink, Offset(w * 0.98f, h * 0.34f), Offset(w * 0.7f, h * 0.66f), w * 0.1f, StrokeCap.Round)
        }
    }
}
