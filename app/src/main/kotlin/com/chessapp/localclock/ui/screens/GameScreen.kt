package com.chessapp.localclock.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.chessapp.engine.Color
import com.chessapp.localclock.ui.components.CapturedPiecesRow
import com.chessapp.localclock.ui.components.ChessBoard
import com.chessapp.localclock.ui.components.ClockDisplay
import com.chessapp.localclock.ui.components.GameOverDialog
import com.chessapp.localclock.ui.components.MoveHistoryRow
import com.chessapp.localclock.ui.components.PromotionDialog
import com.chessapp.localclock.viewmodel.GameUiState
import com.chessapp.localclock.viewmodel.GameViewModel

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onBackToSetup: () -> Unit
) {
    val uiState = viewModel.uiState
    var showResignConfirm by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> viewModel.pauseClock()
                Lifecycle.Event.ON_START -> viewModel.resumeClock()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        PlayerBar(
            name = "Black",
            capturedOf = Color.WHITE,
            millisRemaining = uiState.clock.blackMillisRemaining,
            isActive = uiState.clock.activeColor == Color.BLACK && !uiState.isGameOver,
            isFlagged = uiState.clock.flaggedColor == Color.BLACK,
            uiState = uiState
        )

        Spacer(Modifier.height(12.dp))
        ChessBoard(
            uiState = uiState,
            onSquareTapped = viewModel::onSquareTapped,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        PlayerBar(
            name = "White",
            capturedOf = Color.BLACK,
            millisRemaining = uiState.clock.whiteMillisRemaining,
            isActive = uiState.clock.activeColor == Color.WHITE && !uiState.isGameOver,
            isFlagged = uiState.clock.flaggedColor == Color.WHITE,
            uiState = uiState
        )

        Spacer(Modifier.height(12.dp))
        MoveHistoryRow(
            moves = uiState.position.moveHistory,
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
        )

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { showResignConfirm = true },
                modifier = Modifier.weight(1f),
                enabled = !uiState.isGameOver
            ) {
                Text("Resign")
            }
            OutlinedButton(onClick = onBackToSetup, modifier = Modifier.weight(1f)) {
                Text("New setup")
            }
        }
    }

    uiState.pendingPromotion?.let {
        PromotionDialog(
            color = uiState.position.sideToMove,
            onChoose = viewModel::choosePromotion,
            onDismiss = viewModel::cancelSelection
        )
    }

    if (uiState.isGameOver) {
        GameOverDialog(
            uiState = uiState,
            onRematch = viewModel::rematch,
            onBackToSetup = onBackToSetup
        )
    }

    if (showResignConfirm) {
        val resigningColor = uiState.position.sideToMove
        AlertDialog(
            onDismissRequest = { showResignConfirm = false },
            title = { Text("Resign?") },
            text = { Text("${if (resigningColor == Color.WHITE) "White" else "Black"} will lose the game.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resign(resigningColor)
                    showResignConfirm = false
                }) { Text("Resign") }
            },
            dismissButton = {
                TextButton(onClick = { showResignConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun PlayerBar(
    name: String,
    capturedOf: Color,
    millisRemaining: Long,
    isActive: Boolean,
    isFlagged: Boolean,
    uiState: GameUiState
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(name, style = MaterialTheme.typography.labelLarge)
            CapturedPiecesRow(captured = uiState.capturedPieces(capturedOf), color = capturedOf)
        }
        ClockDisplay(
            label = name,
            millisRemaining = millisRemaining,
            isActive = isActive,
            isUnlimited = uiState.clock.isUnlimited,
            isFlagged = isFlagged
        )
    }
}
