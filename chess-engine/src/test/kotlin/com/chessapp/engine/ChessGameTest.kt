package com.chessapp.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun ChessGame.play(from: String, to: String, promotion: PieceType? = null): Move {
    val fromSq = Square.fromAlgebraic(from)
    val toSq = Square.fromAlgebraic(to)
    val candidates = legalMovesFrom(fromSq).filter { it.to == toSq && it.promotion == promotion }
    assertTrue(candidates.isNotEmpty(), "No legal move $from-$to (promotion=$promotion). Legal from $from: ${legalMovesFrom(fromSq)}")
    val move = candidates.first()
    assertTrue(makeMove(move), "makeMove rejected $from-$to")
    return move
}

class ChessGameTest {

    @Test
    fun `initial position has 20 legal moves for white`() {
        val game = ChessGame()
        assertEquals(20, game.allLegalMoves().size)
        assertEquals(Color.WHITE, game.sideToMove)
        assertEquals(GameStatus.ONGOING, game.status())
    }

    @Test
    fun `after 1 e4 e5 there are 29 legal moves for white`() {
        val game = ChessGame()
        game.play("e2", "e4")
        assertEquals(Color.BLACK, game.sideToMove)
        game.play("e7", "e5")
        assertEquals(Color.WHITE, game.sideToMove)
        // Known perft-adjacent figure for this well-studied position.
        assertEquals(29, game.allLegalMoves().size)
    }

    @Test
    fun `pawn cannot jump over a blocking piece and has no diagonal move without a capture`() {
        val board = Board.empty()
        board.setPiece(Square.fromAlgebraic("e1"), Piece(Color.WHITE, PieceType.KING))
        board.setPiece(Square.fromAlgebraic("e8"), Piece(Color.BLACK, PieceType.KING))
        board.setPiece(Square.fromAlgebraic("e2"), Piece(Color.WHITE, PieceType.PAWN))
        board.setPiece(Square.fromAlgebraic("e3"), Piece(Color.BLACK, PieceType.PAWN))
        val state = GameState(
            board = board,
            sideToMove = Color.WHITE,
            castlingRights = CastlingRights(false, false, false, false),
            enPassantTarget = null,
            halfMoveClock = 0,
            fullMoveNumber = 1
        )
        val game = ChessGame(state)
        assertTrue(game.legalMovesFrom(Square.fromAlgebraic("e2")).isEmpty())
    }

    @Test
    fun `fools mate is checkmate in two moves each`() {
        val game = ChessGame()
        game.play("f2", "f3")
        game.play("e7", "e5")
        game.play("g2", "g4")
        game.play("d8", "h4")
        assertEquals(GameStatus.CHECKMATE, game.status())
        assertTrue(game.isInCheck(Color.WHITE))
        assertTrue(game.allLegalMoves().isEmpty())
    }

    @Test
    fun `scholars mate is checkmate`() {
        val game = ChessGame()
        game.play("e2", "e4")
        game.play("e7", "e5")
        game.play("f1", "c4")
        game.play("b8", "c6")
        game.play("d1", "h5")
        game.play("g8", "f6") // blunder: does not defend f7
        game.play("h5", "f7")
        assertEquals(GameStatus.CHECKMATE, game.status())
    }

    @Test
    fun `stalemate is detected and is not checkmate`() {
        // Classic K+Q vs K stalemate: black king a8, white king c7, white queen b6 to move played by black -> black stalemated.
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
        assertFalse(game.isInCheck(Color.BLACK))
        assertTrue(game.allLegalMoves().isEmpty())
        assertEquals(GameStatus.STALEMATE, game.status())
    }

    @Test
    fun `castling kingside and queenside are legal when squares are clear`() {
        val board = Board.empty()
        board.setPiece(Square.fromAlgebraic("e1"), Piece(Color.WHITE, PieceType.KING))
        board.setPiece(Square.fromAlgebraic("a1"), Piece(Color.WHITE, PieceType.ROOK))
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
        val kingMoves = game.legalMovesFrom(Square.fromAlgebraic("e1"))
        assertTrue(kingMoves.any { it.flag == MoveFlag.CASTLE_KINGSIDE })
        assertTrue(kingMoves.any { it.flag == MoveFlag.CASTLE_QUEENSIDE })

        val castled = game.play("e1", "g1")
        assertEquals(MoveFlag.CASTLE_KINGSIDE, castled.flag)
        assertEquals(Piece(Color.WHITE, PieceType.ROOK), game.state.board.pieceAt(Square.fromAlgebraic("f1")))
        assertNull(game.state.board.pieceAt(Square.fromAlgebraic("h1")))
    }

    @Test
    fun `castling is illegal through check`() {
        val board = Board.empty()
        board.setPiece(Square.fromAlgebraic("e1"), Piece(Color.WHITE, PieceType.KING))
        board.setPiece(Square.fromAlgebraic("h1"), Piece(Color.WHITE, PieceType.ROOK))
        board.setPiece(Square.fromAlgebraic("e8"), Piece(Color.BLACK, PieceType.KING))
        // black rook attacks f1, the square the king must pass through
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
    fun `en passant capture works and clears the captured pawn`() {
        val game = ChessGame()
        game.play("e2", "e4")
        game.play("a7", "a6")
        game.play("e4", "e5")
        game.play("d7", "d5") // black double pawn push next to white pawn on e5
        val epMove = game.play("e5", "d6")
        assertEquals(MoveFlag.EN_PASSANT, epMove.flag)
        assertNull(game.state.board.pieceAt(Square.fromAlgebraic("d5")))
        assertEquals(Piece(Color.WHITE, PieceType.PAWN), game.state.board.pieceAt(Square.fromAlgebraic("d6")))
    }

    @Test
    fun `pawn promotion offers all four piece choices and converts the pawn`() {
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
        assertEquals(setOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT), promoMoves.map { it.promotion }.toSet())

        game.play("a7", "a8", PieceType.QUEEN)
        assertEquals(Piece(Color.WHITE, PieceType.QUEEN), game.state.board.pieceAt(Square.fromAlgebraic("a8")))
    }

    @Test
    fun `king and king is insufficient material`() {
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

    @Test
    fun `capturedPieces reflects material taken off the board`() {
        val game = ChessGame()
        game.play("e2", "e4")
        game.play("d7", "d5")
        game.play("e4", "d5") // white captures black pawn
        assertEquals(listOf(PieceType.PAWN), game.capturedPieces(Color.BLACK))
        assertTrue(game.capturedPieces(Color.WHITE).isEmpty())
    }

    @Test
    fun `promoting a pawn does not count it as captured`() {
        val game = ChessGame()
        game.play("a2", "a4"); game.play("h7", "h6")
        game.play("a4", "a5"); game.play("h6", "h5")
        game.play("a5", "a6"); game.play("h5", "h4")
        game.play("a6", "b7"); game.play("h4", "h3") // white captures b7 pawn
        game.play("b7", "a8", PieceType.QUEEN) // captures a8 rook and promotes
        assertTrue(game.capturedPieces(Color.WHITE).isEmpty(), "White lost no piece: ${game.capturedPieces(Color.WHITE)}")
        assertEquals(listOf(PieceType.ROOK, PieceType.PAWN), game.capturedPieces(Color.BLACK))
        // White: 39 - pawn + promoted queen = 47; Black: 39 - pawn - rook = 33.
        assertEquals(14, game.materialAdvantage(Color.WHITE))
        assertEquals(-14, game.materialAdvantage(Color.BLACK))
    }

    @Test
    fun `starting position repeated three times is a draw`() {
        val game = ChessGame()
        fun knightShuffle() {
            game.play("g1", "f3"); game.play("g8", "f6")
            game.play("f3", "g1"); game.play("f6", "g8")
        }
        knightShuffle() // starting position seen twice
        assertEquals(GameStatus.ONGOING, game.status())
        knightShuffle() // ...and a third time
        assertEquals(GameStatus.DRAW_REPETITION, game.status())
    }

    @Test
    fun `moving into check is rejected as illegal`() {
        val board = Board.empty()
        board.setPiece(Square.fromAlgebraic("e1"), Piece(Color.WHITE, PieceType.KING))
        board.setPiece(Square.fromAlgebraic("e8"), Piece(Color.BLACK, PieceType.ROOK))
        board.setPiece(Square.fromAlgebraic("h8"), Piece(Color.BLACK, PieceType.KING))
        val state = GameState(
            board = board,
            sideToMove = Color.WHITE,
            castlingRights = CastlingRights(false, false, false, false),
            enPassantTarget = null,
            halfMoveClock = 0,
            fullMoveNumber = 1
        )
        val game = ChessGame(state)
        val kingMoves = game.legalMovesFrom(Square.fromAlgebraic("e1"))
        // King cannot step onto the e-file (still attacked by the rook) nor stay in check.
        assertFalse(kingMoves.any { it.to == Square.fromAlgebraic("e2") })
        assertTrue(kingMoves.any { it.to == Square.fromAlgebraic("d2") })
    }
}
