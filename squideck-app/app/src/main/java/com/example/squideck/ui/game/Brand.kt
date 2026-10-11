package com.example.squideck.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.squideck.domain.Symbol
import com.example.squideck.ui.theme.Pink

/** Circle, triangle, square: the game's mark, in the guards' pink. */
@Composable
fun BrandSymbols(size: Dp, modifier: Modifier = Modifier, color: Color = Pink) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(size * 0.35f)) {
        listOf(Symbol.Circle, Symbol.Triangle, Symbol.Square).forEach {
            SymbolIcon(it, Modifier.size(size), color = color)
        }
    }
}

/** Logo: stacked and large for the first screen, inline and small for headers. */
@Composable
fun BrandMark(modifier: Modifier = Modifier, large: Boolean = false) {
    if (large) {
        Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            BrandSymbols(34.dp)
            Text("SQUIDECK", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, letterSpacing = 6.sp)
        }
    } else {
        Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BrandSymbols(14.dp)
            Text("SQUIDECK", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
        }
    }
}
