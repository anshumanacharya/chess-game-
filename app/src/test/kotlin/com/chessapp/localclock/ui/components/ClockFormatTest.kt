package com.chessapp.localclock.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class ClockFormatTest {

    @Test
    fun formatsMinutesAndSecondsAboveTenSeconds() {
        assertEquals("10:00", formatClockTime(600_000))
        assertEquals("9:58", formatClockTime(598_400))
    }

    @Test
    fun showsTenthsUnderTenSecondsAndNeverGoesNegative() {
        assertEquals("0:07.4", formatClockTime(7_450))
        assertEquals("0:00.0", formatClockTime(-120))
    }

    @Test
    fun lcdGhostLightsEverySegmentWithTheSameShape() {
        // Same length and punctuation as the lit text, so the two layers line up exactly.
        assertEquals("88:88", lcdGhostText("10:00"))
        assertEquals("8:88", lcdGhostText("9:58"))
        assertEquals("8:88.8", lcdGhostText("0:07.4"))
        assertEquals("88:88", lcdGhostText("--:--"))
    }
}
