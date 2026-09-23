package com.chessapp.engine

/** Conventional material value of a piece type in pawns (the king is never traded, so 0). */
val PieceType.points: Int
    get() = when (this) {
        PieceType.PAWN -> 1
        PieceType.KNIGHT -> 3
        PieceType.BISHOP -> 3
        PieceType.ROOK -> 5
        PieceType.QUEEN -> 9
        PieceType.KING -> 0
    }

/**
 * Pieces of [color] the opponent has captured so far, most valuable first. Read from the move
 * history rather than inferred from what's missing on the board, so a promotion (one pawn fewer,
 * one queen more) isn't mistaken for a captured pawn.
 */
fun GameState.capturedPieces(color: Color): List<PieceType> =
    moveHistory
        .mapNotNull { move -> move.capturedPiece?.takeIf { it.color == color }?.type }
        .sortedByDescending { it.ordinal } // QUEEN, ROOK, BISHOP, KNIGHT, PAWN

/**
 * [color]'s material minus the opponent's, counting the pieces actually on the board — so a
 * promoted piece counts at its full value. Positive means [color] is ahead (the classic "+N").
 */
fun GameState.materialAdvantage(color: Color): Int =
    board.allPieces().sumOf { (_, piece) -> if (piece.color == color) piece.type.points else -piece.type.points }
