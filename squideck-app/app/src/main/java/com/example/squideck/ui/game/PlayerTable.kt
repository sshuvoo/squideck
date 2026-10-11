package com.example.squideck.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

/** One seat at the table: who sits there and what to show under them. */
data class SeatInfo(
    val player: Player,
    val subtitle: String,
    val timer: TurnTimer?,
    val isFinished: Boolean,
    val isLocal: Boolean = false,
    /** True for the player who receives the local player's pass this turn. */
    val isRecipient: Boolean = false,
)

/**
 * Lays every seat evenly around a felt table, the local player at the bottom and the rest clockwise in
 * turn order. [center] is drawn in the middle of the table.
 */
@Composable
fun PlayerTable(
    seats: List<SeatInfo>,
    localSeat: Int,
    onSeatPositioned: (seat: Int, center: Offset) -> Unit,
    modifier: Modifier = Modifier,
    center: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        // Bounded by width, and by height so the bottom seat's labels always fit.
        val radius = minOf((maxWidth - SeatWidth) / 2, (maxHeight - SeatHeight) / 1.85f)
        // Seats reach from the top pair's rings (~0.81r above centre) down to the local seat's labels (r + 80dp below),
        // so the drawing is pulled up by half the difference to sit centred, and the middle text sits midway between
        // the top seats' labels and the local seat's ring.
        val balance = radius * 0.0955f + 18.dp
        val shift = -balance
        TableFelt(Modifier.size(radius * 2).offset(y = shift))
        Column(Modifier.width(radius * 1.05f).offset(y = shift + balance), horizontalAlignment = Alignment.CenterHorizontally) { center() }
        seats.forEach { seat ->
            val turnsFromLocal = (seat.player.seat - localSeat).mod(seats.size)
            val angle = Math.toRadians(90.0 + FULL_TURN / seats.size * turnsFromLocal)
            Seat(
                seat,
                // The local player's cards fly to and from their hand, so their seat is not an anchor.
                onAvatarPositioned = if (seat.isLocal) null else { position -> onSeatPositioned(seat.player.seat, position) },
                modifier = Modifier.offset(
                    x = radius * cos(angle).toFloat(),
                    y = radius * sin(angle).toFloat() + (SeatHeight - RingSize) / 2 + shift,
                ),
            )
        }
    }
}

@Composable
private fun TableFelt(modifier: Modifier = Modifier) {
    val felt = MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier) {
        val r = size.minDimension / 2
        drawCircle(Brush.radialGradient(listOf(felt.copy(alpha = 0.75f), felt.copy(alpha = 0.25f)), center, r), r)
        drawCircle(MaskWhite.copy(alpha = 0.12f), r, style = Stroke(2.dp.toPx()))
        drawCircle(MaskWhite.copy(alpha = 0.05f), r * 0.7f, style = Stroke(1.dp.toPx()))
    }
}

@Composable
private fun Seat(seat: SeatInfo, onAvatarPositioned: ((Offset) -> Unit)?, modifier: Modifier = Modifier) {
    val remaining = rememberRemainingMs(seat.timer)
    val secondsText by remember(seat.timer) { derivedStateOf { "${secondsLeft(remaining.value)}s" } }
    Column(
        modifier.width(SeatWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(
            Modifier.size(RingSize).then(
                if (onAvatarPositioned != null) {
                    Modifier.onGloballyPositioned { onAvatarPositioned(it.positionInRoot() + Offset(it.size.width / 2f, it.size.height / 2f)) }
                } else Modifier,
            ),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.matchParentSize()) {
                val stroke = RingStroke.toPx()
                val inset = stroke / 2
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val ringColor = when {
                    seat.isRecipient -> Pink.copy(alpha = 0.6f)
                    seat.timer != null -> MaskWhite.copy(alpha = 0.15f)
                    else -> MaskWhite.copy(alpha = 0.3f)
                }
                drawArc(ringColor, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(if (seat.timer != null || seat.isRecipient) stroke else 1.5.dp.toPx()))
                if (seat.timer != null) {
                    val left = remaining.value
                    drawArc(
                        countdownColor(left), -90f, 360f * left / seat.timer.durationMs, false, Offset(inset, inset), arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
            }
            if (seat.isLocal) InitialBadge(seat.player.name, size = AvatarSize)
            else AnimeAvatar(AvatarStyles.forSeat(seat.player.seat), Modifier.size(AvatarSize))
        }
        Text(
            if (seat.isLocal) "You" else seat.player.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        when {
            seat.timer != null -> Text(secondsText, style = MaterialTheme.typography.labelSmall, color = countdownColor(remaining.value), fontWeight = FontWeight.Bold)
            seat.isFinished -> Pill(seat.subtitle, filled = true)
            seat.isRecipient -> Pill("RECEIVES", filled = false)
            else -> Text(seat.subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Pill(text: String, filled: Boolean) {
    val shape = RoundedCornerShape(50)
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Black,
        letterSpacing = 0.5.sp,
        color = if (filled) Color.White else Pink,
        modifier = (if (filled) Modifier.background(Pink, shape) else Modifier.border(1.dp, Pink, shape))
            .padding(horizontal = 8.dp, vertical = 1.dp),
    )
}
