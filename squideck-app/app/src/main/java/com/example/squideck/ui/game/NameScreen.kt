package com.example.squideck.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.squideck.domain.profile.PlayerName
import com.example.squideck.ui.components.GameButton
import com.example.squideck.ui.theme.MaskWhite
import com.example.squideck.ui.theme.Pink

/** First screen: who are you? The name is what other players see at the table. */
@Composable
fun NameScreen(initialName: String, onContinue: (String) -> Unit, modifier: Modifier = Modifier) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    val valid = PlayerName.isValid(name)
    Column(
        modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("SQUIDECK", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
        Text("What should we call you?", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(PlayerName.MAX_LENGTH) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Your name") },
            supportingText = { Text("${PlayerName.MIN_LENGTH}–${PlayerName.MAX_LENGTH} characters") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (valid) onContinue(name) }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Pink,
                unfocusedBorderColor = MaskWhite.copy(alpha = 0.3f),
                focusedLabelColor = Pink,
                cursorColor = Pink,
            ),
        )
        GameButton("Continue", { onContinue(name) }, Modifier.fillMaxWidth(), enabled = valid)
    }
}
