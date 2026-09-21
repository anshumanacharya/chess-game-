package com.chessapp.localclock.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chessapp.engine.Color
import com.chessapp.engine.PieceType

@Composable
fun PromotionDialog(color: Color, onChoose: (PieceType) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Promote pawn to…") },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (type in listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)) {
                    TextButton(onClick = { onChoose(type) }) {
                        Text(pieceGlyph(type, color), fontSize = 32.sp)
                    }
                }
            }
        },
        confirmButton = {}
    )
}
