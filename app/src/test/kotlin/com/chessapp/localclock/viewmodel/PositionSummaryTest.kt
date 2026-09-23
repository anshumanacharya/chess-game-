package com.chessapp.localclock.viewmodel

import com.chessapp.engine.Color
import com.chessapp.engine.GameState
import com.chessapp.engine.GameStatus
import com.chessapp.engine.MoveGenerator
import com.chessapp.engine.PieceType
import com.chessapp.engine.Square
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class PositionSummaryTest {

    private fun GameState.play(vararg moves: String): GameState = moves.fold(this) { state, uci ->
        val from = Square.fromAlgebraic(uci.substring(0, 2))
        val to = Square.fromAlgebraic(uci.substring(2, 4))
        val move = MoveGenerator.legalMovesFrom(state, from).first { it.to == to }
        MoveGenerator.applyMove(state, move)
    }

    @Test
    fun newGameSummaryIsOngoingWithNothingCaptured() {
        val summary = PositionSummary(GameState.newGame())
        assertEquals(GameStatus.ONGOING, summary.status)
        assertNull(summary.lastMove)
        assertNull(summary.kingInCheck)
        assertEquals(emptyList(), summary.capturedPieces(Color.WHITE))
        assertEquals(0, summary.materialAdvantage(Color.WHITE))
    }

    @Test
    fun foolsMateReportsCheckmateAndTheMatedKingSquare() {
        val summary = PositionSummary(GameState.newGame().play("f2f3", "e7e5", "g2g4", "d8h4"))
        assertEquals(GameStatus.CHECKMATE, summary.status)
        assertEquals(Square.fromAlgebraic("e1"), summary.kingInCheck)
        assertEquals(Square.fromAlgebraic("h4"), summary.lastMove?.to)
    }

    @Test
    fun capturesAndMaterialAreReportedFromBothSides() {
        val summary = PositionSummary(GameState.newGame().play("e2e4", "d7d5", "e4d5"))
        assertEquals(listOf(PieceType.PAWN), summary.capturedPieces(Color.BLACK))
        assertEquals(emptyList(), summary.capturedPieces(Color.WHITE))
        assertEquals(1, summary.materialAdvantage(Color.WHITE))
        assertEquals(-1, summary.materialAdvantage(Color.BLACK))
    }

    @Test
    fun copyingUiStateForAClockTickKeepsTheSameSummary() {
        // What makes a tick cheap: GameUiState.copy(clock = ...) shares the summary instead of
        // re-deriving status/captures, and ChessBoard's inputs are the same instances.
        val state = GameUiState(summary = PositionSummary(GameState.newGame().play("e2e4")))
        val ticked = state.copy(clock = state.clock.tick(100))
        assertSame(state.summary, ticked.summary)
        assertSame(state.position, ticked.position)
    }
}
