package com.example.squideck.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.squideck.ui.theme.OnDark
import com.example.squideck.ui.theme.Pink

enum class GameButtonStyle { Primary, Secondary, Ghost }

enum class GameButtonSize(val height: Dp, val depth: Dp, val corner: Dp, val fontSize: Int, val horizontalPadding: Dp) {
    Large(height = 56.dp, depth = 6.dp, corner = 16.dp, fontSize = 17, horizontalPadding = 20.dp),
    Small(height = 40.dp, depth = 4.dp, corner = 12.dp, fontSize = 13, horizontalPadding = 14.dp),
}

private class ButtonColors(
    val faceTop: Color,
    val faceBottom: Color,
    val edge: Color,
    val border: Color,
    val content: Color,
)

private fun colorsFor(style: GameButtonStyle, enabled: Boolean): ButtonColors = when {
    style == GameButtonStyle.Ghost -> ButtonColors(Color.Transparent, Color.Transparent, Color.Transparent, Color.Transparent, if (enabled) OnDark.copy(alpha = 0.75f) else OnDark.copy(alpha = 0.3f))
    !enabled -> ButtonColors(Color(0xFF2B3C39), Color(0xFF223330), Color(0xFF111C1A), Color.White.copy(alpha = 0.06f), OnDark.copy(alpha = 0.45f))
    style == GameButtonStyle.Primary -> ButtonColors(Color(0xFFFF4D85), Pink, Color(0xFF8F0F3C), Color.Transparent, Color.White)
    else -> ButtonColors(Color(0xFF24665B), Color(0xFF184B42), Color(0xFF081F1B), Color.White.copy(alpha = 0.2f), OnDark)
}

/**
 * The game's one button: a chunky key with a darker bottom lip that presses down, glossy face,
 * bold uppercase label and a light haptic tick. Pass [progress] (0..1, read at draw time) to turn
 * it into a countdown button whose fill drains.
 */
@Composable
fun GameButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: GameButtonStyle = GameButtonStyle.Primary,
    size: GameButtonSize = GameButtonSize.Large,
    enabled: Boolean = true,
    leading: String? = null,
    progress: (() -> Float)? = null,
    progressColor: () -> Color = { Pink },
) {
    val colors = colorsFor(style, enabled)
    val ghost = style == GameButtonStyle.Ghost
    val depth = if (ghost) 0.dp else size.depth
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val travel by animateDpAsState(
        if (pressed && enabled) depth else 0.dp,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "buttonTravel",
    )
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(size.corner)

    // Wraps its label unless the caller fixes the width (fillMaxWidth, weight, width).
    BoxWithConstraints(
        modifier
            .height(size.height + depth)
            .alpha(if (ghost && pressed) 0.6f else 1f)
            .drawBehind {
                if (!ghost) {
                    val lip = depth.toPx()
                    drawRoundRect(
                        colors.edge,
                        topLeft = Offset(0f, lip),
                        size = Size(this.size.width, this.size.height - lip),
                        cornerRadius = CornerRadius(size.corner.toPx()),
                    )
                }
            }
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
    ) {
        val fixedWidth = constraints.hasBoundedWidth && constraints.minWidth == constraints.maxWidth
        Box(
            Modifier
                .then(if (fixedWidth) Modifier.fillMaxWidth() else Modifier)
                .height(size.height)
                .offset(y = travel)
                .clip(shape)
                .drawBehind {
                    if (!ghost) {
                        val track = if (progress != null) lerp(colors.faceBottom, Color.Black, 0.55f) else null
                        drawRect(if (track != null) Brush.verticalGradient(listOf(track, track)) else Brush.verticalGradient(listOf(colors.faceTop, colors.faceBottom)))
                        if (progress != null) {
                            val fill = progressColor().copy(alpha = if (enabled) 1f else 0.55f)
                            drawRect(
                                Brush.verticalGradient(listOf(lerp(fill, Color.White, 0.25f), fill)),
                                size = Size(this.size.width * progress().coerceIn(0f, 1f), this.size.height),
                            )
                        }
                        // glossy upper half
                        drawRect(Color.White.copy(alpha = 0.10f), Offset.Zero, Size(this.size.width, this.size.height * 0.45f))
                    }
                }
                .then(if (colors.border != Color.Transparent) Modifier.border(BorderStroke(1.5.dp, colors.border), shape) else Modifier)
                .padding(horizontal = size.horizontalPadding),
            contentAlignment = Alignment.Center,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (leading != null) Text(leading, style = labelStyle(size, colors.content))
                Text(text.uppercase(), style = labelStyle(size, colors.content), maxLines = 1)
            }
        }
    }
}

private fun labelStyle(size: GameButtonSize, color: Color) = TextStyle(
    color = color,
    fontSize = size.fontSize.sp,
    fontWeight = FontWeight.Black,
    letterSpacing = 1.sp,
    shadow = Shadow(Color.Black.copy(alpha = 0.35f), Offset(0f, 3f), 2f),
)
