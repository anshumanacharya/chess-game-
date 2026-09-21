package com.chessapp.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ClockStateTest {

    @Test
    fun `unlimited clock never ticks down`() {
        var clock = ClockState.from(ClockConfig.UNLIMITED).start(Color.WHITE)
        clock = clock.tick(10_000)
        assertEquals(0, clock.whiteMillisRemaining)
        assertNull(clock.flaggedColor)
    }

    @Test
    fun `ticking subtracts only from the active player`() {
        var clock = ClockState.from(ClockConfig.preset(5, 0)).start(Color.WHITE)
        clock = clock.tick(1_000)
        assertEquals(5 * 60_000L - 1000, clock.whiteMillisRemaining)
        assertEquals(5 * 60_000L, clock.blackMillisRemaining)
    }

    @Test
    fun `completing a move adds increment and switches the active player`() {
        var clock = ClockState.from(ClockConfig.preset(3, 2)).start(Color.WHITE)
        clock = clock.tick(5_000)
        clock = clock.onMoveCompleted(movedColor = Color.WHITE, nextColor = Color.BLACK)
        assertEquals(3 * 60_000L - 5000 + 2000, clock.whiteMillisRemaining)
        assertEquals(Color.BLACK, clock.activeColor)
    }

    @Test
    fun `running out of time flags that player and stops the clock`() {
        var clock = ClockState.from(ClockConfig.preset(1, 0)).start(Color.WHITE)
        clock = clock.tick(61_000)
        assertEquals(0, clock.whiteMillisRemaining)
        assertEquals(Color.WHITE, clock.flaggedColor)
        assertNull(clock.activeColor)
        // further ticks are no-ops once flagged
        val after = clock.tick(1_000)
        assertEquals(clock, after)
    }

    @Test
    fun `unlimited config reports isUnlimited`() {
        assertTrue(ClockState.from(ClockConfig.UNLIMITED).isUnlimited)
    }
}
