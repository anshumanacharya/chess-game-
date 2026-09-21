package com.chessapp.localclock.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.chessapp.engine.Color
import com.chessapp.localclock.viewmodel.GameOverReason
import com.chessapp.localclock.viewmodel.GameUiState

@Composable
fun GameOverDialog(
    uiState: GameUiState,
    onRematch: () -> Unit,
    onBackToSetup: () -> Unit
) {
    val reason = uiState.gameOverReason ?: return
    val title = when (uiState.winner) {
        Color.WHITE -> "White wins"
        Color.BLACK -> "Black wins"
        null -> "Draw"
    }
    val subtitle = when (reason) {
        GameOverReason.CHECKMATE -> "Checkmate"
        GameOverReason.STALEMATE -> "Stalemate"
        GameOverReason.DRAW_FIFTY_MOVE -> "Draw – 50-move rule"
        GameOverReason.DRAW_REPETITION -> "Draw – threefold repetition"
        GameOverReason.DRAW_INSUFFICIENT_MATERIAL -> "Draw – insufficient material"
        GameOverReason.WHITE_TIME_OUT -> "White ran out of time"
        GameOverReason.BLACK_TIME_OUT -> "Black ran out of time"
        GameOverReason.WHITE_RESIGNED -> "White resigned"
        GameOverReason.BLACK_RESIGNED -> "Black resigned"
    }
    AlertDialog(
        onDismissRequest = {},
        title = { Text(title) },
        text = { Text(subtitle) },
        confirmButton = {
            TextButton(onClick = onRematch) { Text("Rematch") }
        },
        dismissButton = {
            TextButton(onClick = onBackToSetup) { Text("New setup") }
        }
    )
}
