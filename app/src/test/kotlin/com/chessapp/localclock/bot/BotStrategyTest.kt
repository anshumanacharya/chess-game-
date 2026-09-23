package com.chessapp.localclock.bot

import com.chessapp.engine.GameState
import com.chessapp.engine.Move
import com.chessapp.engine.MoveGenerator
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BotStrategyTest {

    private class FakeSource(private val move: Move?) : BotSource {
        var calls = 0
        var closed = false
        override suspend fun chooseMove(state: GameState): Move? {
            calls++
            return move
        }
        override fun close() {
            closed = true
        }
    }

    private val start = GameState.newGame()
    private val e4 = MoveGenerator.legalMoves(start).first { it.to.algebraic == "e4" }
    private val d4 = MoveGenerator.legalMoves(start).first { it.to.algebraic == "d4" }

    @Test
    fun usesTheRemoteSourceWhenOnline() = runBlocking {
        val local = FakeSource(d4)
        val strategy = BotStrategy(local, FakeSource(e4)) { true }
        assertEquals(BotMoveResult(e4, BotMoveOrigin.REMOTE), strategy.chooseMove(start))
        assertEquals(0, local.calls)
    }

    @Test
    fun fallsBackToLocalWhenOfflineOrWhenRemoteFails() = runBlocking {
        val offline = BotStrategy(FakeSource(d4), FakeSource(e4)) { false }
        assertEquals(BotMoveResult(d4, BotMoveOrigin.LOCAL), offline.chooseMove(start))

        val remoteFails = BotStrategy(FakeSource(d4), FakeSource(null)) { true }
        assertEquals(BotMoveResult(d4, BotMoveOrigin.LOCAL), remoteFails.chooseMove(start))
    }

    @Test
    fun closeReleasesBothSources() {
        val local = FakeSource(d4)
        val remote = FakeSource(e4)
        BotStrategy(local, remote) { true }.close()
        assertTrue(local.closed)
        assertTrue(remote.closed)
    }
}
