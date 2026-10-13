package com.example.squideck.ui.game

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.squideck.data.nearby.NearbyPermissions
import com.example.squideck.net.RoomAd
import com.example.squideck.ui.components.GameButton
import com.example.squideck.ui.components.GameButtonSize
import com.example.squideck.ui.components.GameButtonStyle
import com.example.squideck.ui.components.GameText
import com.example.squideck.ui.theme.Pink
import com.example.squideck.ui.theme.Teal
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

/**
 * Nearby play: create a room on this phone, or join one a friend created. Free play, no credits. Looking for
 * rooms runs only while this panel is on screen (it uses the radio), and both actions open the rules first.
 */
@Composable
fun NearbyPanel(
    browse: NearbyBrowse,
    onStartSearch: () -> Unit,
    onStopSearch: () -> Unit,
    onCreateRoom: () -> Unit,
    onJoinRoom: (RoomAd) -> Unit,
) {
    val context = LocalContext.current
    val playServicesReady = remember {
        GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
    }
    var granted by remember { mutableStateOf(NearbyPermissions.granted(context)) }
    var askedOnce by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        askedOnce = true
        granted = NearbyPermissions.granted(context)
    }
    var locationOff by remember { mutableStateOf(NearbyPermissions.locationSwitchOff(context)) }
    // The player may turn Location on or grant permissions in Settings and come back: check again on return.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                granted = NearbyPermissions.granted(context)
                locationOff = NearbyPermissions.locationSwitchOff(context)
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val ready = granted && !locationOff && playServicesReady
    // Search only while the panel is visible and allowed; stop when leaving to save battery.
    DisposableEffect(ready) {
        if (ready) onStartSearch()
        onDispose { onStopSearch() }
    }

    var rulesFor by remember { mutableStateOf<NearbyAction?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        InvitationCard(
            footer = "Tap to create a room",
            onClick = {
                when {
                    !granted -> launcher.launch(NearbyPermissions.required)
                    locationOff -> context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    else -> rulesFor = NearbyAction.Create
                }
            },
            clickDescription = "Create a nearby room",
        )
        GameText(
            "ROOMS NEARBY",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        when {
            !playServicesReady -> Notice("Nearby play needs Google Play services, which isn't available on this phone.")
            !granted -> PermissionNotice(
                deniedBefore = askedOnce,
                onAllow = { launcher.launch(NearbyPermissions.required) },
                onOpenSettings = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                    )
                },
            )
            locationOff -> Notice(
                "On this Android version, finding phones nearby needs Location turned on. Squideck still never uses your location.",
                action = "Turn on Location",
                onAction = { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) },
            )
            browse.error != null -> Notice(browse.error, action = "Try again", onAction = { onStopSearch(); onStartSearch() })
            browse.rooms.isEmpty() -> Searching()
            else -> browse.rooms.forEach { room -> RoomRow(room) { rulesFor = NearbyAction.Join(room) } }
        }
    }

    rulesFor?.let { action ->
        RulesSheet(
            squideckRules(title = when (action) {
                NearbyAction.Create -> "Nearby room"
                is NearbyAction.Join -> "${action.room.creatorName}'s room"
            }),
            onDismiss = { rulesFor = null },
            actionLabel = when (action) {
                NearbyAction.Create -> "Create room"
                is NearbyAction.Join -> "Join room"
            },
            onAction = {
                rulesFor = null
                when (action) {
                    NearbyAction.Create -> onCreateRoom()
                    is NearbyAction.Join -> onJoinRoom(action.room)
                }
            },
        )
    }
}

private sealed interface NearbyAction {
    data object Create : NearbyAction
    data class Join(val room: RoomAd) : NearbyAction
}

@Composable
private fun RoomRow(room: RoomAd, onClick: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            AnimeAvatar(AvatarStyles.byId(room.avatarId), Modifier.size(48.dp))
            Column(Modifier.weight(1f)) {
                GameText("${room.creatorName}'s room", fontWeight = FontWeight.Bold)
                GameText("Free play", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            GameButton("Join", onClick, style = GameButtonStyle.Secondary, size = GameButtonSize.Small)
        }
    }
}

/** A radar sweep while looking for rooms. */
@Composable
private fun Searching() {
    val pulse by rememberInfiniteTransition(label = "radar").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_600), RepeatMode.Restart),
        label = "radarPulse",
    )
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(48.dp)) {
                drawCircle(Teal.copy(alpha = 1f - pulse), radius = size.minDimension / 2 * pulse, style = Stroke(2.dp.toPx()))
                drawCircle(Teal.copy(alpha = (1f - pulse) * 0.6f), radius = size.minDimension / 2 * (pulse * 0.6f), style = Stroke(2.dp.toPx()))
                drawCircle(Teal, radius = 4.dp.toPx())
            }
        }
        Column {
            GameText("Looking for rooms…", fontWeight = FontWeight.Bold)
            GameText(
                "Ask a friend to open Squideck, go to Nearby and create a room.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PermissionNotice(deniedBefore: Boolean, onAllow: () -> Unit, onOpenSettings: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        GameText(
            "To find friends nearby, Squideck uses Bluetooth and Wi-Fi to talk to their phones. It never uses your location.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        GameButton("Allow nearby devices", onAllow, Modifier.fillMaxWidth(), style = GameButtonStyle.Secondary)
        if (deniedBefore) {
            GameButton("Open settings", onOpenSettings, Modifier.fillMaxWidth(), style = GameButtonStyle.Ghost, size = GameButtonSize.Small)
        }
    }
}

@Composable
private fun Notice(text: String, action: String? = null, onAction: () -> Unit = {}) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        GameText(text, color = Pink)
        if (action != null) GameButton(action, onAction, style = GameButtonStyle.Secondary, size = GameButtonSize.Small, modifier = Modifier.alpha(1f))
    }
}
