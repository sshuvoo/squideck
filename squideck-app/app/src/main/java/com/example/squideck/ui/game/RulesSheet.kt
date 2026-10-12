package com.example.squideck.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.squideck.ui.components.GameButton
import com.example.squideck.ui.components.GameText
import com.example.squideck.ui.theme.Pink

/** One numbered rule in a game's rules sheet. */
data class RuleItem(val title: String, val body: String)

/** The rules of one game. Each game type supplies its own, so every table card can show the right rules. */
data class GameRules(
    val title: String,
    val rules: List<RuleItem>,
    /** Optional artwork above the rules, such as the game's card symbols. */
    val header: (@Composable () -> Unit)? = null,
)

/** Bottom sheet that shows any game's [rules]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesSheet(rules: GameRules, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            GameText(rules.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            rules.header?.invoke()
            rules.rules.forEachIndexed { index, rule -> Rule(index + 1, rule) }
            GameButton("Got it", onDismiss, Modifier.fillMaxWidth().padding(bottom = 16.dp))
        }
    }
}

@Composable
private fun Rule(number: Int, rule: RuleItem) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(28.dp).background(Pink, CircleShape), contentAlignment = Alignment.Center) {
            GameText("$number", fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelLarge)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            GameText(rule.title, fontWeight = FontWeight.Bold)
            GameText(rule.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
