package com.chessapp.localclock.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// A restrained, mostly-monochrome palette: warm neutrals for the board and app chrome,
// one ink accent for interactive/active state, and a single muted red reserved for check.
val BoardLightSquare = Color(0xFFEDEBE6)
val BoardDarkSquare = Color(0xFF8A8478)
val AccentInk = Color(0xFF3A4A5A)
val BoardSelected = AccentInk
val BoardLegalTarget = Color(0xFF7C8A72)
val BoardLastMove = Color(0xFFC9A66B)
val BoardCheck = Color(0xFFB3453D)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9FB0BE),
    onPrimary = Color(0xFF1B1B1B),
    secondary = BoardDarkSquare,
    background = Color(0xFF17181A),
    surface = Color(0xFF201F1D)
)

private val LightColors = lightColorScheme(
    primary = AccentInk,
    onPrimary = Color(0xFFFFFFFF),
    secondary = BoardDarkSquare,
    background = Color(0xFFF7F6F3),
    surface = Color(0xFFFFFFFF)
)

@Composable
fun LocalChessClockTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
