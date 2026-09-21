package com.chessapp.engine

/**
 * Immutable chess-clock snapshot. The Android layer drives [tick] from a coroutine
 * ticker (e.g. every 100ms) and calls [onMoveCompleted] right after a move is made.
 */
data class ClockState(
    val config: ClockConfig,
    val whiteMillisRemaining: Long,
    val blackMillisRemaining: Long,
    val activeColor: Color? = null,
    val flaggedColor: Color? = null
) {
    val isUnlimited: Boolean get() = config.isUnlimited

    fun remaining(color: Color): Long = if (color == Color.WHITE) whiteMillisRemaining else blackMillisRemaining

    /** Subtracts elapsed time from the active player's clock; sets [flaggedColor] if it hits zero. */
    fun tick(elapsedMillis: Long): ClockState {
        if (config.isUnlimited || activeColor == null || flaggedColor != null || elapsedMillis <= 0) return this
        val newWhite = if (activeColor == Color.WHITE) whiteMillisRemaining - elapsedMillis else whiteMillisRemaining
        val newBlack = if (activeColor == Color.BLACK) blackMillisRemaining - elapsedMillis else blackMillisRemaining
        val flagged = when {
            newWhite <= 0 -> Color.WHITE
            newBlack <= 0 -> Color.BLACK
            else -> null
        }
        return copy(
            whiteMillisRemaining = newWhite.coerceAtLeast(0),
            blackMillisRemaining = newBlack.coerceAtLeast(0),
            flaggedColor = flagged,
            activeColor = if (flagged != null) null else activeColor
        )
    }

    /** Adds increment to the player who just moved and hands the clock to `nextColor`. */
    fun onMoveCompleted(movedColor: Color, nextColor: Color): ClockState {
        if (config.isUnlimited) return copy(activeColor = nextColor)
        return if (movedColor == Color.WHITE) {
            copy(whiteMillisRemaining = whiteMillisRemaining + config.incrementMillis, activeColor = nextColor)
        } else {
            copy(blackMillisRemaining = blackMillisRemaining + config.incrementMillis, activeColor = nextColor)
        }
    }

    fun start(color: Color): ClockState = copy(activeColor = color)

    fun pause(): ClockState = copy(activeColor = null)

    companion object {
        fun from(config: ClockConfig): ClockState = ClockState(
            config = config,
            whiteMillisRemaining = config.initialMillis,
            blackMillisRemaining = config.initialMillis
        )
    }
}
