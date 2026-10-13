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
import com.example.squideck.ui.components.CloseButton
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
    /** Optional table facts (players, entry, prizes) shown above the rules. */
    val details: (@Composable () -> Unit)? = null,
    val rulesTitle: String = "How to play",
)

/**
 * Bottom sheet that shows any game's [rules]. Its bottom button runs [onAction] (for a table: join it); the
 * cross closes the sheet without acting.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesSheet(
    rules: GameRules,
    onDismiss: () -> Unit,
    actionLabel: String = "Got it",
    actionEnabled: Boolean = true,
    onAction: () -> Unit = onDismiss,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                GameText(rules.title, Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                CloseButton(onDismiss)
            }
            rules.details?.invoke()
            GameText(rules.rulesTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Pink)
            rules.header?.invoke()
            rules.rules.forEachIndexed { index, rule -> Rule(index + 1, rule) }
            GameButton(actionLabel, onAction, Modifier.fillMaxWidth().padding(bottom = 16.dp), enabled = actionEnabled)
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
