package com.example.squideck.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.squideck.domain.Player
import com.example.squideck.domain.TurnTimer
import com.example.squideck.ui.theme.MaskWhite
import com.example.squideck.ui.theme.Pink
import kotlin.math.cos
import kotlin.math.sin

private val SeatWidth = 96.dp
private val SeatHeight = 124.dp
private val RingSize = 88.dp
private val RingStroke = 4.dp
private val AvatarSize = 76.dp
private const val FULL_TURN = 360.0

/** One opponent's slot: avatar, name and either a card count or the finishing place. */
data class SeatInfo(val player: Player, val subtitle: String, val timer: TurnTimer?, val isFinished: Boolean)

/**
 * Lays [opponents] evenly around a circle with the local player's slot at the bottom, in turn order
 * (clockwise on screen). [center] is drawn inside the ring.
 */
@Composable
fun PlayerTable(
    opponents: List<SeatInfo>,
    localSeat: Int,
    seatCount: Int,
    onSeatPositioned: (seat: Int, center: Offset) -> Unit,
    modifier: Modifier = Modifier,
    center: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val diameter = minOf(maxWidth, maxHeight)
        val radius = (diameter - SeatWidth) / 2
        Canvas(Modifier.size(radius * 2)) {
            drawCircle(
                MaskWhite.copy(alpha = 0.12f),
                radius = size.minDimension / 2,
                style = Stroke(width = 2.dp.toPx()),
            )
        }
        Column(Modifier.width(radius * 1.1f).offset(y = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) { center() }
        opponents.forEach { seat ->
            val turnsFromLocal = (seat.player.seat - localSeat).mod(seatCount)
            val angle = Math.toRadians(90.0 + FULL_TURN / seatCount * turnsFromLocal)
            OpponentSeat(
                seat,
                onAvatarPositioned = { onSeatPositioned(seat.player.seat, it) },
                modifier = Modifier.offset(
                    x = radius * cos(angle).toFloat(),
                    y = radius * sin(angle).toFloat() + (SeatHeight - RingSize) / 2,
                ),
            )
        }
    }
}

@Composable
private fun OpponentSeat(seat: SeatInfo, onAvatarPositioned: (Offset) -> Unit, modifier: Modifier = Modifier) {
    val remaining = rememberRemainingMs(seat.timer)
    val secondsText by remember(seat.timer) { derivedStateOf { "${secondsLeft(remaining.value)}s" } }
    Column(
        modifier.width(SeatWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(
            Modifier.size(RingSize).onGloballyPositioned {
                onAvatarPositioned(it.positionInRoot() + Offset(it.size.width / 2f, it.size.height / 2f))
            },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.matchParentSize()) {
                val stroke = RingStroke.toPx()
                val inset = stroke / 2
                val arcSize = Size(size.width - stroke, size.height - stroke)
                drawArc(MaskWhite.copy(alpha = if (seat.timer != null) 0.15f else 0.3f), 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(if (seat.timer != null) stroke else 1.5.dp.toPx()))
                if (seat.timer != null) {
                    val left = remaining.value
                    drawArc(
                        countdownColor(left), -90f, 360f * left / seat.timer.durationMs, false, Offset(inset, inset), arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
            }
            AnimeAvatar(AvatarStyles.forSeat(seat.player.seat), Modifier.size(AvatarSize))
        }
        Text(
            seat.player.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            if (seat.timer != null) secondsText else seat.subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = when {
                seat.isFinished -> MaskWhite
                seat.timer != null -> countdownColor(remaining.value)
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            textAlign = TextAlign.Center,
            modifier = if (seat.isFinished) {
                Modifier.background(Pink, RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 1.dp)
            } else Modifier,
        )
    }
}
