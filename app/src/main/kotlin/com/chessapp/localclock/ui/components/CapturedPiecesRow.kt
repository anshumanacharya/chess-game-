package com.chessapp.localclock.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chessapp.engine.Color
import com.chessapp.engine.PieceType
import com.chessapp.localclock.ui.theme.BoardLightSquare

/** Shows the icons of pieces of [color] that have been captured (in the order given — the
 *  engine's `capturedPieces` lists them most valuable first), and the classic +N
 *  material-advantage count when [advantage] is positive. */
@Composable
fun CapturedPiecesRow(captured: List<PieceType>, color: Color, advantage: Int = 0, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // On a board-light swatch rather than the screen background: Black's pieces all but
        // disappear against the dark-theme surface (mirrors web's .captured-pieces).
        if (captured.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .background(BoardLightSquare, RoundedCornerShape(6.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                for (type in captured) {
                    Image(
                        painter = painterResource(id = pieceIconRes(type, color)),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
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
