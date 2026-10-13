package com.example.squideck.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

/*
 * Squid Game brand palette: the ONLY colours this app may use (six brand colours plus the invitation-card paper).
 *
 * Everything on screen is one of these six, a translucent version of one (`copy(alpha = …)`), or one laid
 * over another with [tint] / [shade]. Never write `Color(0x…)` or `Color.Red`-style constants anywhere else.
 * Mirrored in res/values/colors.xml for the window background and launcher icon; keep the two in sync.
 */
val Crimson = Color(0xFFAF0B45)
val Pink = Color(0xFFF44786)
val White = Color(0xFFFFFFFF)
val Teal = Color(0xFF249F9C)
val DarkTeal = Color(0xFF037A76)
val Ink = Color(0xFF161313)

/** Kraft paper of the show's invitation card; used only for the invitation-style game cards. */
val Kraft = Color(0xFFD2AE84)

/** A lighter version of a palette colour: [White] laid over it at [amount] opacity. */
fun Color.tint(amount: Float): Color = White.copy(alpha = amount).compositeOver(this)

/** A darker version of a palette colour: [Ink] laid over it at [amount] opacity. */
fun Color.shade(amount: Float): Color = Ink.copy(alpha = amount).compositeOver(this)
