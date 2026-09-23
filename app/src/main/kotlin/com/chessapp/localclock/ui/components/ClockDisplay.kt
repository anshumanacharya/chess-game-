package com.chessapp.localclock.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chessapp.localclock.R
import com.chessapp.localclock.ui.theme.LcdBezel
import com.chessapp.localclock.ui.theme.LcdDim
import com.chessapp.localclock.ui.theme.LcdLit
import com.chessapp.localclock.ui.theme.LcdPanel

fun formatClockTime(millis: Long): String {
    val clamped = millis.coerceAtLeast(0)
    return if (clamped < 10_000) {
        val seconds = clamped / 1000
        val tenths = (clamped % 1000) / 100
        "0:%02d.%d".format(seconds, tenths)
    } else {
        val totalSeconds = clamped / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        "%d:%02d".format(minutes, seconds)
    }
}

/** Every segment of every digit position — what a real LCD shows faintly when unlit. Same shape
 *  as the lit text (":" and "." kept), so the two line up character for character. */
fun lcdGhostText(text: String): String = text.map { if (it.isDigit() || it == '-') '8' else it }.joinToString("")

/** DSEG7 Classic (SIL Open Font License): a seven-segment LCD face, mirrors web/fonts/. */
private val LcdFontFamily = FontFamily(Font(R.font.dseg7_classic_bold, FontWeight.Bold))

/**
 * A red seven-segment LCD clock (mirrors web's .clock-pill): lit and glowing while this side's
 * clock runs, dim while it waits, blinking once flagged. No color label: position (top/bottom,
 * matching the board orientation) and the captured pieces shown alongside it already say whose
 * clock this is.
 */
@Composable
fun ClockDisplay(
    millisRemaining: Long,
    isActive: Boolean,
    isUnlimited: Boolean,
    isFlagged: Boolean,
    modifier: Modifier = Modifier
) {
    // The LCD font has no "∞"; dashes are the conventional idle LCD readout.
    val text = if (isUnlimited) "--:--" else formatClockTime(millisRemaining)
    val lit = isActive || isFlagged
    val digitsVisible = if (isFlagged) {
        // Off for the first half of each second, on for the second — so if system animations
        // are disabled (the transition jumps straight to its end value) the digits stay shown.
        val phase by rememberInfiniteTransition(label = "lcd-blink").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(durationMillis = 1000, easing = LinearEasing)),
            label = "lcd-blink-phase"
        )
        phase >= 0.5f
    } else {
        true
    }
    val digitStyle = TextStyle(
        fontFamily = LcdFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        color = if (lit) LcdLit else LcdDim,
        shadow = if (lit) Shadow(color = LcdLit.copy(alpha = 0.85f), blurRadius = 16f) else null
    )
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .widthIn(min = 110.dp)
            .clip(shape)
            .background(LcdPanel)
            .border(2.dp, LcdBezel, shape)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Box {
            Text(lcdGhostText(text), style = digitStyle.copy(color = LcdLit.copy(alpha = 0.1f), shadow = null))
            if (digitsVisible) Text(text, style = digitStyle)
        }
    }
}
