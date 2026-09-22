package com.chessapp.localclock.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chessapp.engine.Color
import com.chessapp.engine.PieceType

fun pieceValue(type: PieceType): Int = when (type) {
    PieceType.QUEEN -> 9
    PieceType.ROOK -> 5
    PieceType.BISHOP -> 3
    PieceType.KNIGHT -> 3
    PieceType.PAWN -> 1
    PieceType.KING -> 0
}

fun materialValue(captured: List<PieceType>): Int = captured.sumOf(::pieceValue)

/** Shows the icons of pieces of [color] that have been captured (highest value first), and the
 *  classic +N material-advantage count when [advantage] is positive. */
@Composable
fun CapturedPiecesRow(captured: List<PieceType>, color: Color, advantage: Int = 0, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        for (type in captured.sortedByDescending(::pieceValue)) {
            Image(
                painter = painterResource(id = pieceIconRes(type, color)),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
        if (advantage > 0) {
            Text(
                text = "+$advantage",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
