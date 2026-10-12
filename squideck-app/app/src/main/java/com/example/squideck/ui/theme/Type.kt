package com.example.squideck.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.example.squideck.R

/**
 * "Game Of Squids" by Darrell Flood, a free fan font inspired by the series logo (not an official Netflix font;
 * licence in assets/licenses/game-of-squids-license.txt). It is the only typeface in the app.
 */
val GameFont = FontFamily(Font(R.font.game_of_squids))

private fun TextStyle.inGameFont() = copy(fontFamily = GameFont)

private val Default = Typography()

/** Every Material text style uses the game font. */
val Typography = Typography(
    displayLarge = Default.displayLarge.inGameFont(),
    displayMedium = Default.displayMedium.inGameFont(),
    displaySmall = Default.displaySmall.inGameFont(),
    headlineLarge = Default.headlineLarge.inGameFont(),
    headlineMedium = Default.headlineMedium.inGameFont(),
    headlineSmall = Default.headlineSmall.inGameFont(),
    titleLarge = Default.titleLarge.inGameFont(),
    titleMedium = Default.titleMedium.inGameFont(),
    titleSmall = Default.titleSmall.inGameFont(),
    bodyLarge = Default.bodyLarge.inGameFont(),
    bodyMedium = Default.bodyMedium.inGameFont(),
    bodySmall = Default.bodySmall.inGameFont(),
    labelLarge = Default.labelLarge.inGameFont(),
    labelMedium = Default.labelMedium.inGameFont(),
    labelSmall = Default.labelSmall.inGameFont(),
)
