package com.chessapp.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Covers the same critical rules-engine scenarios as chess-engine's ChessGameTest, under
// JS-legal (no-space) function names, to confirm the shared source behaves identically when
// compiled for the JS target instead of the JVM one.

private fun ChessGame.play(from: String, to: String, promotion: PieceType? = null): Move {
    val fromSq = Square.fromAlgebraic(from)
    val toSq = Square.fromAlgebraic(to)
    val move = legalMovesFrom(fromSq).first { it.to == toSq && it.promotion == promotion }
    assertTrue(makeMove(move), "makeMove rejected $from-$to")
    return move
}

class EngineOnJsTest {

    @Test
    fun initialPositionHas20LegalMoves() {
        val game = ChessGame()
        assertEquals(20, game.allLegalMoves().size)
        assertEquals(GameStatus.ONGOING, game.status())
    }

    @Test
    fun foolsMateIsCheckmate() {
        val game = ChessGame()
        game.play("f2", "f3")
        game.play("e7", "e5")
        game.play("g2", "g4")
        game.play("d8", "h4")
        assertEquals(GameStatus.CHECKMATE, game.status())
        assertTrue(game.allLegalMoves().isEmpty())
    }

    @Test
    fun castlingKingsideIsLegalWhenSquaresAreClear() {
        val board = Board.empty()
        board.setPiece(Square.fromAlgebraic("e1"), Piece(Color.WHITE, PieceType.KING))
        board.setPiece(Square.fromAlgebraic("h1"), Piece(Color.WHITE, PieceType.ROOK))
        board.setPiece(Square.fromAlgebraic("e8"), Piece(Color.BLACK, PieceType.KING))
        val state = GameState(
            board = board,
            sideToMove = Color.WHITE,
            castlingRights = CastlingRights(),
            enPassantTarget = null,
            halfMoveClock = 0,
            fullMoveNumber = 1
        )
        val game = ChessGame(state)
        val castled = game.play("e1", "g1")
        assertEquals(MoveFlag.CASTLE_KINGSIDE, castled.flag)
        assertEquals(Piece(Color.WHITE, PieceType.ROOK), game.state.board.pieceAt(Square.fromAlgebraic("f1")))
    }

    @Test
    fun castlingIsIllegalThroughCheck() {
        val board = Board.empty()
        board.setPiece(Square.fromAlgebraic("e1"), Piece(Color.WHITE, PieceType.KING))
        board.setPiece(Square.fromAlgebraic("h1"), Piece(Color.WHITE, PieceType.ROOK))
        board.setPiece(Square.fromAlgebraic("e8"), Piece(Color.BLACK, PieceType.KING))
        board.setPiece(Square.fromAlgebraic("f8"), Piece(Color.BLACK, PieceType.ROOK))
        val state = GameState(
            board = board,
            sideToMove = Color.WHITE,
            castlingRights = CastlingRights(),
            enPassantTarget = null,
            halfMoveClock = 0,
            fullMoveNumber = 1
        )
        val game = ChessGame(state)
        val kingMoves = game.legalMovesFrom(Square.fromAlgebraic("e1"))
        assertFalse(kingMoves.any { it.flag == MoveFlag.CASTLE_KINGSIDE })
    }

    @Test
    fun enPassantCaptureClearsTheCapturedPawn() {
        val game = ChessGame()
        game.play("e2", "e4")
        game.play("a7", "a6")
        game.play("e4", "e5")
        game.play("d7", "d5")
        val epMove = game.play("e5", "d6")
        assertEquals(MoveFlag.EN_PASSANT, epMove.flag)
        assertNull(game.state.board.pieceAt(Square.fromAlgebraic("d5")))
    }

    @Test
    fun pawnPromotionOffersAllFourPieceChoices() {
        val board = Board.empty()
        board.setPiece(Square.fromAlgebraic("a7"), Piece(Color.WHITE, PieceType.PAWN))
        board.setPiece(Square.fromAlgebraic("e1"), Piece(Color.WHITE, PieceType.KING))
        board.setPiece(Square.fromAlgebraic("e8"), Piece(Color.BLACK, PieceType.KING))
        val state = GameState(
            board = board,
            sideToMove = Color.WHITE,
            castlingRights = CastlingRights(false, false, false, false),
            enPassantTarget = null,
            halfMoveClock = 0,
            fullMoveNumber = 1
        )
        val game = ChessGame(state)
        val promoMoves = game.legalMovesFrom(Square.fromAlgebraic("a7")).filter { it.to == Square.fromAlgebraic("a8") }
        assertEquals(4, promoMoves.size)
        game.play("a7", "a8", PieceType.QUEEN)
        assertEquals(Piece(Color.WHITE, PieceType.QUEEN), game.state.board.pieceAt(Square.fromAlgebraic("a8")))
    }

    @Test
    fun stalemateIsDetectedAndIsNotCheckmate() {
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
        val game = ChessGame(state)
        assertTrue(game.allLegalMoves().isEmpty())
        assertEquals(GameStatus.STALEMATE, game.status())
    }

    @Test
    fun kingAndKingIsInsufficientMaterial() {
        val board = Board.empty()
        board.setPiece(Square.fromAlgebraic("e1"), Piece(Color.WHITE, PieceType.KING))
        board.setPiece(Square.fromAlgebraic("e8"), Piece(Color.BLACK, PieceType.KING))
        val state = GameState(
            board = board,
            sideToMove = Color.WHITE,
            castlingRights = CastlingRights(false, false, false, false),
            enPassantTarget = null,
            halfMoveClock = 0,
            fullMoveNumber = 1
        )
        val game = ChessGame(state)
        assertEquals(GameStatus.DRAW_INSUFFICIENT_MATERIAL, game.status())
    }
}
