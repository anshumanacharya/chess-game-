package com.chessapp.engine

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Covers the same scenarios as chess-engine's ChessBotTest, under JS-legal (no-space) function
// names, to confirm ChessBot behaves identically when compiled for the JS target.

private fun ChessGame.play(from: String, to: String, promotion: PieceType? = null): Move {
    val fromSq = Square.fromAlgebraic(from)
    val toSq = Square.fromAlgebraic(to)
    val move = legalMovesFrom(fromSq).first { it.to == toSq && it.promotion == promotion }
    assertTrue(makeMove(move), "makeMove rejected $from-$to")
    return move
}

class ChessBotOnJsTest {

    @Test
    fun chooseMoveReturnsNullWhenThereIsNoLegalMove() {
        val board = Board.empty()
        board.setPiece(Square.fromAlgebraic("a8"), Piece(Color.BLACK, PieceType.KING))
        board.setPiece(Square.fromAlgebraic("c7"), Piece(Color.WHITE, PieceType.KING))
        board.setPiece(Square.fromAlgebraic("b6"), Piece(Color.WHITE, PieceType.QUEEN))
        val state = GameState(
            board = board,
            sideToMove = Color.BLACK,
            castlingRights = CastlingRights(false, false, false, false),
            enPassantTarget = null,
            halfMoveClock = 0,
            fullMoveNumber = 1
        )
        val bot = ChessBot()
        assertNull(bot.chooseMove(state))
    }

    @Test
    fun botCapturesAHangingPieceWithNoiseAndBlundersDisabled() {
        val board = Board.empty()
        board.setPiece(Square.fromAlgebraic("a1"), Piece(Color.WHITE, PieceType.KING))
        board.setPiece(Square.fromAlgebraic("h2"), Piece(Color.WHITE, PieceType.QUEEN))
        board.setPiece(Square.fromAlgebraic("a8"), Piece(Color.BLACK, PieceType.KING))
        board.setPiece(Square.fromAlgebraic("h8"), Piece(Color.BLACK, PieceType.ROOK))
        val state = GameState(
            board = board,
            sideToMove = Color.WHITE,
            castlingRights = CastlingRights(false, false, false, false),
            enPassantTarget = null,
            halfMoveClock = 0,
            fullMoveNumber = 1
        )
        val bot = ChessBot(blunderProbability = 0.0, noiseCentipawns = 0)
        val move = bot.chooseMove(state)
        assertNotNull(move)
        assertTrue(move.isCapture)
        assertEquals(Square.fromAlgebraic("h8"), move.to)
    }

    @Test
    fun botDeliversAnAvailableMateInOneWithNoiseAndBlundersDisabled() {
        val game = ChessGame()
        game.play("f2", "f3")
        game.play("e7", "e5")
        game.play("g2", "g4")
        // Black to move: Qh4# is on the board.
        val bot = ChessBot(blunderProbability = 0.0, noiseCentipawns = 0)
        val move = bot.chooseMove(game.state)
        assertNotNull(move)
        assertTrue(game.makeMove(move))
        assertEquals(GameStatus.CHECKMATE, game.status())
    }

    @Test
    fun botOnlyEverPlaysLegalMovesAcrossASimulatedGame() {
        val game = ChessGame()
        val bot = ChessBot(random = Random(42))
        var plies = 0
        while (plies < 30 && !game.status().isGameOver) {
            val move = bot.chooseMove(game.state) ?: break
            assertTrue(move in MoveGenerator.legalMoves(game.state))
            assertTrue(game.makeMove(move))
            plies++
        }
        assertTrue(plies > 0)
    }
}
