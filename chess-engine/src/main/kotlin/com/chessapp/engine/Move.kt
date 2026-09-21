package com.chessapp.engine

enum class MoveFlag {
    NORMAL, DOUBLE_PAWN_PUSH, EN_PASSANT, CASTLE_KINGSIDE, CASTLE_QUEENSIDE, PROMOTION
}

/**
 * A fully-specified move. `capturedSquare` differs from `to` only for en passant,
 * where the captured pawn sits on a different square than the destination.
 */
data class Move(
    val from: Square,
    val to: Square,
    val piece: Piece,
    val capturedPiece: Piece? = null,
    val capturedSquare: Square? = null,
    val promotion: PieceType? = null,
    val flag: MoveFlag = MoveFlag.NORMAL
) {
    val isCapture: Boolean get() = capturedPiece != null

    /** Simplified algebraic notation (no check/mate suffix, no disambiguation). */
    fun toShortAlgebraic(): String {
        if (flag == MoveFlag.CASTLE_KINGSIDE) return "O-O"
        if (flag == MoveFlag.CASTLE_QUEENSIDE) return "O-O-O"

        val pieceLetter = when (piece.type) {
            PieceType.PAWN -> ""
            PieceType.KNIGHT -> "N"
            PieceType.BISHOP -> "B"
            PieceType.ROOK -> "R"
            PieceType.QUEEN -> "Q"
            PieceType.KING -> "K"
        }
        val fromFile = if (piece.type == PieceType.PAWN && isCapture) "${'a' + from.file}" else ""
        val captureMark = if (isCapture) "x" else ""
        val promo = promotion?.let {
            "=" + when (it) {
                PieceType.QUEEN -> "Q"
                PieceType.ROOK -> "R"
                PieceType.BISHOP -> "B"
                PieceType.KNIGHT -> "N"
                else -> ""
            }
        } ?: ""
        return "$pieceLetter$fromFile$captureMark${to.algebraic}$promo"
    }
}
