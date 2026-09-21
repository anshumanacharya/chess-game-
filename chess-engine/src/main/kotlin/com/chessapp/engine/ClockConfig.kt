package com.chessapp.engine

/**
 * A chess clock time control. The clock is fully optional: [isUnlimited] means no clock
 * is shown/ticking at all, per-player time never runs out.
 */
data class ClockConfig(
    val initialMillis: Long,
    val incrementMillis: Long = 0,
    val isUnlimited: Boolean = false
) {
    companion object {
        val UNLIMITED = ClockConfig(initialMillis = 0, incrementMillis = 0, isUnlimited = true)

        fun preset(minutes: Int, incrementSeconds: Int = 0): ClockConfig =
            ClockConfig(initialMillis = minutes * 60_000L, incrementMillis = incrementSeconds * 1000L)

        /** Common presets shown on the setup screen. */
        val PRESETS: List<Pair<String, ClockConfig>> = listOf(
            "1 min" to preset(1, 0),
            "3 | 2" to preset(3, 2),
            "5 min" to preset(5, 0),
            "10 min" to preset(10, 0),
            "15 | 10" to preset(15, 10),
            "30 min" to preset(30, 0)
        )
    }
}
