package com.example.squideck.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.squideck.ui.components.GameText
import com.example.squideck.ui.theme.Crimson
import com.example.squideck.ui.theme.Ink
import com.example.squideck.ui.theme.Pink
import com.example.squideck.ui.theme.Teal
import com.example.squideck.ui.theme.White
import com.example.squideck.ui.theme.shade
import com.example.squideck.ui.theme.tint
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.launch

private val TrackHeight = 64.dp
/** The guard's head is a little taller than wide. */
private val MaskSize = 42.dp
/** Leaves a gap above and below the head inside the rail; the hood is drawn with equal space top and bottom. */
private val HeadHeight = 52.dp
private val TrackPadding = 6.dp

/**
 * A two-way switch whose thumb is a Squid Game guard's mask. The neon shape on the mask is a circle on the left
 * and a triangle on the right, and it melts from one into the other as the mask slides. Drag or flick the mask,
 * or tap a side; it snaps into place with a neon flare and a haptic thud, and that side's label lights up.
 */
@Composable
fun GuardMaskSwitch(
    leftLabel: String,
    rightLabel: String,
    /** 0 = left (circle), 1 = right (triangle). */
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val currentOnSelect by rememberUpdatedState(onSelect)
    // Mask position along the track, 0 (left) to 1 (right); drives the slide, the morph and the label glow.
    val position = remember { Animatable(selected.toFloat()) }
    val flare = remember { Animatable(0f) }
    LaunchedEffect(selected) {
        if (position.targetValue != selected.toFloat()) position.animateTo(selected.toFloat(), snapSpring)
    }

    fun land(side: Int) {
        scope.launch {
            launch { position.animateTo(side.toFloat(), snapSpring) }
            flare.snapTo(1f)
            flare.animateTo(0f, tween(650))
        }
        if (side != selected) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            currentOnSelect(side)
        }
    }

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(TrackHeight)
            // The head's hood and glow stay inside the rail's rounded ends.
            .clip(RoundedCornerShape(percent = 50))
            .semantics {
                role = Role.Switch
                stateDescription = if (selected == 0) leftLabel else rightLabel
                onClick { land(1 - selected); true }
            },
    ) {
        val density = LocalDensity.current
        val travel = with(density) { (maxWidth - MaskSize - TrackPadding * 2).toPx() }
        val p = position.value

        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(selected) { detectTapGestures { land(if (it.x < size.width / 2f) 0 else 1) } }
                .pointerInput(selected, travel) {
                    detectHorizontalDragGestures(
                        onDragEnd = { land(if (position.value > 0.5f) 1 else 0) },
                        onDragCancel = { land(selected) },
                    ) { change, dx ->
                        change.consume()
                        scope.launch { position.snapTo((position.value + dx / travel).coerceIn(0f, 1f)) }
                    }
                },
        ) { drawTrack(p) }

        // Labels sit between the two mask slots; the one the mask is on glows pink.
        GameText(
            leftLabel,
            Modifier.align(Alignment.CenterStart).padding(start = MaskSize + TrackPadding + 14.dp),
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, letterSpacing = 2.sp,
            color = lerp(Pink.tint(0.15f), Teal.copy(alpha = 0.55f), p),
        )
        GameText(
            rightLabel,
            Modifier.align(Alignment.CenterEnd).padding(end = MaskSize + TrackPadding + 14.dp),
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, letterSpacing = 2.sp,
            color = lerp(Teal.copy(alpha = 0.55f), Pink.tint(0.15f), p),
        )

        Canvas(
            Modifier
                .padding(start = TrackPadding)
                .align(Alignment.CenterStart)
                .offset { IntOffset((travel * p).roundToInt(), 0) }
                .size(MaskSize, HeadHeight),
        ) { drawGuardMask(morph = p, flare = flare.value) }
    }
}

private val snapSpring = spring<Float>(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)

/** Recessed dark rail, with dim ghosts of the circle and triangle waiting in the two mask slots. */
private fun DrawScope.drawTrack(p: Float) {
    val radius = CornerRadius(size.height / 2f)
    drawRoundRect(Brush.verticalGradient(listOf(Ink.copy(alpha = 0.85f), Ink.copy(alpha = 0.5f))), cornerRadius = radius)
    drawRoundRect(White.copy(alpha = 0.08f), cornerRadius = radius, style = Stroke(1.dp.toPx()))
    // Inner shadow along the top edge: the rail is carved into the screen.
    drawRoundRect(
        Brush.verticalGradient(listOf(Ink.copy(alpha = 0.7f), Color.Transparent), 0f, 10.dp.toPx()),
        cornerRadius = radius,
    )
    val slot = MaskSize.toPx()
    val pad = TrackPadding.toPx()
    val shapeRadius = slot * 0.22f
    // Ghosts fade out as the mask arrives on top of them.
    drawMorphShape(Offset(pad + slot / 2f, size.height / 2f), shapeRadius, 0f, Teal.copy(alpha = 0.35f * p), 1.6.dp.toPx())
    drawMorphShape(Offset(size.width - pad - slot / 2f, size.height / 2f), shapeRadius, 1f, Teal.copy(alpha = 0.35f * (1f - p)), 1.6.dp.toPx())
}

/**
 * A Squid Game guard's head: pink hood, black mesh mask with seams, and the rank symbol in pale grey on the
 * forehead (a circle at [morph] 0, a triangle at 1). [flare] adds a pink glow right after the switch lands.
 */
private fun DrawScope.drawGuardMask(morph: Float, flare: Float) {
    val w = size.width
    val h = size.height
    // Pink glow on the track under the head, brighter right after it lands.
    drawOval(
        Brush.radialGradient(listOf(Pink.copy(alpha = 0.25f + 0.4f * flare), Color.Transparent), Offset(w / 2f, h * 0.55f), w * (0.75f + 0.2f * flare)),
        Offset(-w * 0.2f, h * 0.05f), Size(w * 1.4f, h),
    )

    // Hood: the same dome at the top and the bottom (mirrored about the middle), lit from above.
    val hood = Path().apply {
        moveTo(w * 0.14f, h * 0.24f)
        cubicTo(w * 0.24f, h * 0.02f, w * 0.76f, h * 0.02f, w * 0.86f, h * 0.24f)
        cubicTo(w * 0.94f, h * 0.4f, w * 0.94f, h * 0.6f, w * 0.86f, h * 0.76f)
        cubicTo(w * 0.76f, h * 0.98f, w * 0.24f, h * 0.98f, w * 0.14f, h * 0.76f)
        cubicTo(w * 0.06f, h * 0.6f, w * 0.06f, h * 0.4f, w * 0.14f, h * 0.24f)
        close()
    }
    drawPath(hood, Brush.verticalGradient(listOf(Pink.tint(0.12f), Pink, Crimson), 0f, h))
    // Fabric folds along the sides of the hood.
    drawPath(Path().apply { moveTo(w * 0.12f, h * 0.34f); quadraticTo(w * 0.09f, h * 0.5f, w * 0.12f, h * 0.66f) }, Crimson.copy(alpha = 0.55f), style = Stroke(1.4.dp.toPx(), cap = StrokeCap.Round))
    drawPath(Path().apply { moveTo(w * 0.88f, h * 0.34f); quadraticTo(w * 0.91f, h * 0.5f, w * 0.88f, h * 0.66f) }, Crimson.copy(alpha = 0.55f), style = Stroke(1.4.dp.toPx(), cap = StrokeCap.Round))

    // Mask: a tall oval centred in the hood, as round at the chin as at the top.
    val mask = Path().apply { addOval(Rect(Offset(w * 0.19f, h * 0.15f), Size(w * 0.62f, h * 0.7f))) }
    // The hood's shadowed inner edge around the face.
    drawPath(mask, Crimson.shade(0.45f), style = Stroke(3.dp.toPx()))
    drawPath(mask, Brush.radialGradient(listOf(Ink.tint(0.16f), Ink), Offset(w * 0.4f, h * 0.3f), w * 0.6f))
    clipPath(mask) {
        // Fine mesh, as on the real masks.
        val step = 2.2.dp.toPx()
        var x = 0f
        while (x < w) { drawLine(White.copy(alpha = 0.06f), Offset(x, 0f), Offset(x, h), 0.6f); x += step }
        var y = 0f
        while (y < h) { drawLine(White.copy(alpha = 0.06f), Offset(0f, y), Offset(w, y), 0.6f); y += step }
        // Seam band under the symbol, and the seam from it down to the chin.
        drawLine(Ink.tint(0.2f), Offset(0f, h * 0.52f), Offset(w, h * 0.52f), 2.dp.toPx())
        drawLine(Ink.copy(alpha = 0.9f), Offset(0f, h * 0.52f + 1.4.dp.toPx()), Offset(w, h * 0.52f + 1.4.dp.toPx()), 0.8.dp.toPx())
        drawLine(Ink.tint(0.14f), Offset(w * 0.5f, h * 0.53f), Offset(w * 0.5f, h), 1.dp.toPx())
        // Soft sheen on the upper left of the mask.
        drawOval(Brush.radialGradient(listOf(White.copy(alpha = 0.12f), Color.Transparent), Offset(w * 0.36f, h * 0.24f), w * 0.3f), Offset(w * 0.1f, h * 0.05f), Size(w * 0.5f, h * 0.4f))
    }

    // Zip from the chin to the bottom of the hood.
    drawLine(Ink.copy(alpha = 0.85f), Offset(w * 0.5f, h * 0.85f), Offset(w * 0.5f, h * 0.92f), 1.6.dp.toPx())
    drawRoundRect(Ink.tint(0.25f), Offset(w * 0.47f, h * 0.86f), Size(w * 0.06f, h * 0.04f), CornerRadius(1.dp.toPx()))

    // Rank symbol on the forehead: pale grey with a faint mesh glow, pinker while the switch flares.
    val symbolCenter = Offset(w * 0.5f, h * 0.33f)
    val symbolRadius = w * 0.15f
    val line = 2.4.dp.toPx()
    drawMorphShape(symbolCenter, symbolRadius, morph, Pink.copy(alpha = 0.45f * flare), line * 3f)
    drawMorphShape(symbolCenter, symbolRadius, morph, lerp(White.copy(alpha = 0.82f), Pink.tint(0.5f), flare), line)
}

/**
 * Outline that melts from a circle (0) into an upward triangle (1). Each point keeps its angle and blends its
 * distance from the centre between the circle's radius and the triangle's edge in that direction.
 */
private fun DrawScope.drawMorphShape(center: Offset, radius: Float, t: Float, color: Color, width: Float) {
    if (color.alpha <= 0.01f) return
    val apothem = radius * 0.62f
    val sector = 2.0 * PI / 3.0
    // A triangle's centroid sits low, so the triangle is nudged down to look centred on the mask.
    val cy = center.y + radius * 0.31f * t
    val path = Path()
    val steps = 90
    for (i in 0..steps) {
        val angle = -PI / 2.0 + i * 2.0 * PI / steps
        // Angle from the nearest edge's normal; the edge is that far away divided by its cosine (vertex up).
        val fromNormal = ((angle + PI / 2.0) % sector + sector) % sector - sector / 2.0
        val toEdge = (apothem / cos(fromNormal)).toFloat()
        val r = radius + (toEdge - radius) * t
        val x = center.x + (r * cos(angle)).toFloat()
        val y = cy + (r * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
}
