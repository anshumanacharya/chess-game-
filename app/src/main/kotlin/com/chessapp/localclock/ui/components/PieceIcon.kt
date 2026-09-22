package com.chessapp.localclock.ui.components

import androidx.annotation.DrawableRes
import com.chessapp.engine.Color
import com.chessapp.engine.Piece
import com.chessapp.engine.PieceType
import com.chessapp.localclock.R

/** Real piece artwork (the Cburnett set — see res/drawable's piece_*.xml) instead of Unicode
 *  glyphs: clearer at a glance, and scales cleanly to any size since it's vector art. */
@DrawableRes
fun pieceIconRes(piece: Piece): Int = pieceIconRes(piece.type, piece.color)

@DrawableRes
fun pieceIconRes(type: PieceType, color: Color): Int = when (color) {
    Color.WHITE -> when (type) {
        PieceType.KING -> R.drawable.piece_wk
        PieceType.QUEEN -> R.drawable.piece_wq
        PieceType.ROOK -> R.drawable.piece_wr
        PieceType.BISHOP -> R.drawable.piece_wb
        PieceType.KNIGHT -> R.drawable.piece_wn
        PieceType.PAWN -> R.drawable.piece_wp
    }
    Color.BLACK -> when (type) {
        PieceType.KING -> R.drawable.piece_bk
        PieceType.QUEEN -> R.drawable.piece_bq
        PieceType.ROOK -> R.drawable.piece_br
        PieceType.BISHOP -> R.drawable.piece_bb
        PieceType.KNIGHT -> R.drawable.piece_bn
        PieceType.PAWN -> R.drawable.piece_bp
    }
}
