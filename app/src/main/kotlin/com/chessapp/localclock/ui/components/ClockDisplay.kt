package com.chessapp.localclock.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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

/** No color label: position (top/bottom, matching the board orientation) and the captured
 *  pieces shown alongside it already say whose clock this is. */
@Composable
fun ClockDisplay(
    millisRemaining: Long,
    isActive: Boolean,
    isUnlimited: Boolean,
    isFlagged: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isFlagged -> MaterialTheme.colorScheme.error
        isActive -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when {
        isFlagged -> MaterialTheme.colorScheme.onError
        isActive -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(color = backgroundColor, contentColor = contentColor, shape = RoundedCornerShape(12.dp), modifier = modifier) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isUnlimited) "∞" else formatClockTime(millisRemaining),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
