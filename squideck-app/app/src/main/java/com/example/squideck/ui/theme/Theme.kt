package com.example.squideck.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/** Panels and cards: dark teal, mostly Ink. */
private val PanelColor = DarkTeal.shade(0.72f)

/** Raised areas such as the table felt: a little more teal. */
private val RaisedColor = DarkTeal.shade(0.55f)

/** Secondary text: teal-tinted white. */
private val MutedText = Teal.tint(0.45f)

// Every Material role is mapped to the brand palette so no stock Material colour can appear.
private val SquideckColorScheme = darkColorScheme(
    primary = Pink,
    onPrimary = White,
    primaryContainer = Crimson,
    onPrimaryContainer = White,
    inversePrimary = Crimson,
    secondary = Teal,
    onSecondary = White,
    secondaryContainer = DarkTeal,
    onSecondaryContainer = White,
    tertiary = Teal,
    onTertiary = White,
    tertiaryContainer = DarkTeal,
    onTertiaryContainer = White,
    background = Ink,
    onBackground = White,
    surface = PanelColor,
    onSurface = White,
    surfaceVariant = RaisedColor,
    onSurfaceVariant = MutedText,
    surfaceTint = DarkTeal,
    inverseSurface = White,
    inverseOnSurface = Ink,
    error = Crimson,
    onError = White,
    errorContainer = Crimson.shade(0.4f),
    onErrorContainer = White,
    outline = White.copy(alpha = 0.24f),
    outlineVariant = White.copy(alpha = 0.12f),
    scrim = Ink,
    surfaceBright = DarkTeal.shade(0.45f),
    surfaceDim = Ink,
    surfaceContainerLowest = Ink,
    surfaceContainerLow = DarkTeal.shade(0.8f),
    surfaceContainer = PanelColor,
    surfaceContainerHigh = DarkTeal.shade(0.62f),
    surfaceContainerHighest = RaisedColor,
)

@Composable
fun SquideckTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SquideckColorScheme,
        typography = Typography,
        content = content
    )
}
