package com.chessapp.localclock.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BoardLightSquare = Color(0xFFEDD6B0)
val BoardDarkSquare = Color(0xFFB58863)
val BoardSelected = Color(0xFF7FB2E5)
val BoardLastMove = Color(0xFFF6F669)
val BoardLegalTarget = Color(0xFF6FCF97)
val BoardCheck = Color(0xFFE05252)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8AB4F8),
    secondary = Color(0xFFB58863),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF3E6FA8),
    secondary = Color(0xFFB58863),
    background = Color(0xFFFAF7F2),
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
