package com.chessapp.engine

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Perft: counts every legal move sequence to a fixed depth and compares against published totals
 * (chessprogramming.org/Perft_Results). Any move-generation bug — a missed or illegal castle, en
 * passant, promotion, pin or check evasion — changes these numbers, so this is the standard
 * end-to-end check for [MoveGenerator].
 */
class PerftTest {

    private fun perft(state: GameState, depth: Int): Long {
        if (depth == 0) return 1
        val moves = MoveGenerator.legalMoves(state)
        if (depth == 1) return moves.size.toLong()
        return moves.sumOf { perft(MoveGenerator.applyMove(state, it), depth - 1) }
    }

    /** Minimal FEN reader (placement, side to move, castling, en passant) — test-only. */
    private fun fen(fen: String): GameState {
        val (placement, side, castling, ep) = fen.split(" ")
        val board = Board.empty()
        placement.split("/").forEachIndexed { row, rankText ->
            var file = 0
            for (c in rankText) {
                if (c.isDigit()) {
                    file += c.digitToInt()
                } else {
                    val type = when (c.lowercaseChar()) {
                        'p' -> PieceType.PAWN; 'n' -> PieceType.KNIGHT; 'b' -> PieceType.BISHOP
                        'r' -> PieceType.ROOK; 'q' -> PieceType.QUEEN; else -> PieceType.KING
                    }
                    board.setPiece(Square(file, 7 - row), Piece(if (c.isUpperCase()) Color.WHITE else Color.BLACK, type))
                    file++
                }
            }
        }
        return GameState(
            board = board,
            sideToMove = if (side == "w") Color.WHITE else Color.BLACK,
            castlingRights = CastlingRights('K' in castling, 'Q' in castling, 'k' in castling, 'q' in castling),
            enPassantTarget = if (ep == "-") null else Square.fromAlgebraic(ep),
            halfMoveClock = 0,
            fullMoveNumber = 1
        )
    }

    @Test
    fun `starting position`() {
        val start = GameState.newGame()
        assertEquals(listOf(20L, 400L, 8_902L, 197_281L), (1..4).map { perft(start, it) })
    }

    @Test
    fun `kiwipete - castling, pins, promotions and en passant`() {
        val state = fen("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -")
        assertEquals(listOf(48L, 2_039L, 97_862L), (1..3).map { perft(state, it) })
    }

    @Test
    fun `position 3 - en passant discovered checks along the rank`() {
        val state = fen("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - -")
        assertEquals(listOf(14L, 191L, 2_812L, 43_238L), (1..4).map { perft(state, it) })
    }

    @Test
    fun `position 4 - promotions and castling under attack`() {
        val state = fen("r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq -")
        assertEquals(listOf(6L, 264L, 9_467L), (1..3).map { perft(state, it) })
    }
}
