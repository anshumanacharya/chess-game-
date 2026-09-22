package com.chessapp.localclock.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.chessapp.engine.Color
import com.chessapp.engine.PieceType
import com.chessapp.localclock.ui.theme.PromotionDialogBackground
import com.chessapp.localclock.ui.theme.PromotionDialogText

@Composable
fun PromotionDialog(color: Color, onChoose: (PieceType) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Promote pawn to…") },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (type in listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)) {
                    TextButton(onClick = { onChoose(type) }) {
                        Image(
                            painter = painterResource(id = pieceIconRes(type, color)),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {},
        containerColor = PromotionDialogBackground,
        titleContentColor = PromotionDialogText,
        textContentColor = PromotionDialogText
    )
}
