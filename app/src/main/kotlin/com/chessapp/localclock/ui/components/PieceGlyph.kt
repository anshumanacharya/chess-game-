package com.chessapp.localclock.ui.components

import com.chessapp.engine.Color
import com.chessapp.engine.Piece
import com.chessapp.engine.PieceType

fun pieceGlyph(piece: Piece): String = pieceGlyph(piece.type, piece.color)

fun pieceGlyph(type: PieceType, color: Color): String = when (type) {
    PieceType.KING -> if (color == Color.WHITE) "♔" else "♚"
    PieceType.QUEEN -> if (color == Color.WHITE) "♕" else "♛"
    PieceType.ROOK -> if (color == Color.WHITE) "♖" else "♜"
    PieceType.BISHOP -> if (color == Color.WHITE) "♗" else "♝"
    PieceType.KNIGHT -> if (color == Color.WHITE) "♘" else "♞"
    PieceType.PAWN -> if (color == Color.WHITE) "♙" else "♟"
}
