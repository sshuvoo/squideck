package com.example.squideck.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.squideck.domain.profile.PlayerName
import com.example.squideck.ui.components.GameButton
import com.example.squideck.ui.components.GameText
import com.example.squideck.ui.components.UppercaseTransformation
import com.example.squideck.ui.theme.Pink
import com.example.squideck.ui.theme.White

/** First screen: who are you? The name and avatar are what other players see at the table. */
@Composable
fun NameScreen(
    initialName: String,
    initialAvatarId: Int,
    onContinue: (name: String, avatarId: Int) -> Unit,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = onBack != null) { onBack?.invoke() }
    var name by rememberSaveable { mutableStateOf(initialName) }
    var avatarId by rememberSaveable { mutableIntStateOf(initialAvatarId) }
    val valid = PlayerName.isValid(name)
    Column(
        modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BrandMark(Modifier.padding(bottom = 12.dp), large = true)
        GameText("Who are you?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        GameText("Other players see your avatar and name at the table.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        AvatarPicker(avatarId, onPick = { avatarId = it })
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(PlayerName.MAX_LENGTH) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = UppercaseTransformation,
            label = { GameText("Your name") },
            supportingText = { GameText("${PlayerName.MIN_LENGTH}–${PlayerName.MAX_LENGTH} characters") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (valid) onContinue(name, avatarId) }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Pink,
                unfocusedBorderColor = White.copy(alpha = 0.3f),
                focusedLabelColor = Pink,
                cursorColor = Pink,
            ),
        )
        GameButton("Continue", { onContinue(name, avatarId) }, Modifier.fillMaxWidth(), enabled = valid)
    }
}

/** A row of every face; the chosen one grows and gets a pink ring. */
@Composable
private fun AvatarPicker(selected: Int, onPick: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).selectableGroup(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        AvatarStyles.all.indices.forEach { id ->
            val chosen = id == selected
            val scale by animateFloatAsState(if (chosen) 1.15f else 1f, label = "avatar$id")
            AnimeAvatar(
                AvatarStyles.byId(id),
                Modifier
                    .size(52.dp)
                    .scale(scale)
                    .border(if (chosen) 3.dp else 1.dp, if (chosen) Pink else White.copy(alpha = 0.25f), CircleShape)
                    .clip(CircleShape)
                    .selectable(selected = chosen, role = Role.RadioButton) { onPick(id) }
                    .semantics { contentDescription = "Avatar ${id + 1}" },
            )
        }
    }
}
