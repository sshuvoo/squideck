package com.example.squideck.ui.game

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.squideck.ui.components.GameText
import com.example.squideck.ui.intro.SquideckLogo
import com.example.squideck.ui.theme.Ink
import com.example.squideck.ui.theme.Kraft
import com.example.squideck.ui.theme.Pink
import com.example.squideck.ui.theme.shade
import com.example.squideck.ui.theme.tint
import kotlin.random.Random

/** The show's invitation card has these proportions. */
private const val CARD_ASPECT = 1.67f

/**
 * A game table shown as the series' invitation card: kraft paper, the logo at the top, a big circle, triangle
 * and square, and one line of text at the bottom. Tapping the card opens the table's details and rules.
 */
@Composable
fun InvitationCard(
    footer: String,
    onClick: () -> Unit,
    clickDescription: String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        if (pressed) 0.96f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "cardPress",
    )
    val haptic = LocalHapticFeedback.current
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(CARD_ASPECT)
            .graphicsLayer {
                scaleX = press
                scaleY = press
            }
            .shadow(if (pressed) 4.dp else 12.dp, shape, ambientColor = Ink, spotColor = Ink)
            .clip(shape)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .semantics { contentDescription = clickDescription },
    ) {
        val fibres = remember { PaperFibres() }
        Canvas(Modifier.fillMaxSize()) {
            drawPaper(fibres)
            drawCardShapes()
        }
        SquideckLogo(
            Modifier.align(Alignment.TopCenter).padding(top = 14.dp).fillMaxWidth(0.24f).aspectRatio(2.6f),
            lineColor = Ink,
            shapeColor = Pink,
        )
        GameText(
            footer,
            Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
            color = Ink,
            fontWeight = FontWeight.Bold,
            letterSpacing = 3.sp,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

/** Random but stable fibres and specks, so the paper texture does not shimmer between frames. */
private class PaperFibres(seed: Int = 456) {
    private val random = Random(seed)
    val fibres = List(140) { FloatArray(5) { random.nextFloat() } }
    val specks = List(90) { FloatArray(4) { random.nextFloat() } }
}

private fun DrawScope.drawPaper(paper: PaperFibres) {
    drawRect(Kraft)
    // Soft darkening towards the edges, like a printed card.
    drawRect(androidx.compose.ui.graphics.Brush.radialGradient(listOf(Kraft.tint(0.06f), Kraft.shade(0.12f)), center, size.maxDimension * 0.7f))
    paper.fibres.forEach { (x, y, angle, length, tone) ->
        val start = Offset(x * size.width, y * size.height)
        val len = 6f + length * 18f
        val end = start + Offset(len * kotlin.math.cos(angle * 6.28f), len * kotlin.math.sin(angle * 6.28f))
        val color = if (tone > 0.5f) Kraft.shade(0.28f) else Kraft.tint(0.22f)
        drawLine(color.copy(alpha = 0.55f), start, end, strokeWidth = 1.2f)
    }
    paper.specks.forEach { (x, y, r, tone) ->
        val color = if (tone > 0.7f) Ink.copy(alpha = 0.35f) else Kraft.shade(0.35f).copy(alpha = 0.6f)
        drawCircle(color, 0.8f + r * 1.8f, Offset(x * size.width, y * size.height))
    }
}

/** Big circle, triangle and square across the middle of the card, in thick ink. */
private fun DrawScope.drawCardShapes() {
    val shapeSize = size.height * 0.34f
    val gap = shapeSize * 0.18f
    val stroke = Stroke(width = shapeSize * 0.09f, join = StrokeJoin.Miter)
    val inset = stroke.width / 2
    val total = shapeSize * 3 + gap * 2
    var left = (size.width - total) / 2
    val top = size.height * 0.36f
    drawCircle(Ink, shapeSize / 2 - inset, Offset(left + shapeSize / 2, top + shapeSize / 2), style = stroke)
    left += shapeSize + gap
    val triangle = Path().apply {
        moveTo(left + shapeSize / 2, top + inset)
        lineTo(left + shapeSize - inset, top + shapeSize - inset)
        lineTo(left + inset, top + shapeSize - inset)
        close()
    }
    drawPath(triangle, Ink, style = stroke)
    left += shapeSize + gap
    drawRect(Ink, Offset(left + inset, top + inset), Size(shapeSize - inset * 2, shapeSize - inset * 2), style = stroke)
}
