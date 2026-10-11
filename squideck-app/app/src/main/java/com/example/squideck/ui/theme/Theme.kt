package com.example.squideck.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SquideckColorScheme = darkColorScheme(
    primary = Pink,
    onPrimary = OnDark,
    primaryContainer = PinkDark,
    background = Background,
    onBackground = OnDark,
    surface = Surface,
    onSurface = OnDark,
    surfaceVariant = SurfaceHigh,
    onSurfaceVariant = Muted,
)

@Composable
fun SquideckTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SquideckColorScheme,
        typography = Typography,
        content = content
    )
}
