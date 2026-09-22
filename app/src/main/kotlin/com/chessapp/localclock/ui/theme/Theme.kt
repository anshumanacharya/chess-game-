package com.chessapp.localclock.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Board colors match lichess.org's default ("brown") theme — the green legal-move dot it
// replaced had poor contrast on the dark squares; this dark, near-black tone (also lichess's
// own choice) reads clearly on both.
val BoardLightSquare = Color(0xFFF0D9B5)
val BoardDarkSquare = Color(0xFFB58863)
val AccentInk = Color(0xFF3A4A5A)
val BoardSelected = AccentInk
val BoardLegalTarget = Color(0xFF141E0A)
val BoardLastMove = Color(0xFFC9A66B)
val BoardCheck = Color(0xFFB3453D)

// The promotion dialog stays light even in dark theme (mirrors web's .promotion-card): Black's
// piece artwork all but disappears against the dark-theme surface.
val PromotionDialogBackground = Color(0xFFFFFFFF)
val PromotionDialogText = Color(0xFF1B1B1F)

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
