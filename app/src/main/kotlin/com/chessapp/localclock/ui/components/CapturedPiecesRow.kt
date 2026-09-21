package com.chessapp.localclock.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chessapp.engine.Color
import com.chessapp.engine.PieceType

private fun pieceValue(type: PieceType): Int = when (type) {
    PieceType.QUEEN -> 9
    PieceType.ROOK -> 5
    PieceType.BISHOP -> 3
    PieceType.KNIGHT -> 3
    PieceType.PAWN -> 1
    PieceType.KING -> 0
}

/** Shows the glyphs of pieces of [color] that have been captured, highest value first. */
@Composable
fun CapturedPiecesRow(captured: List<PieceType>, color: Color, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        for (type in captured.sortedByDescending(::pieceValue)) {
            Text(text = pieceGlyph(type, color), fontSize = 16.sp)
        }
    }
}
