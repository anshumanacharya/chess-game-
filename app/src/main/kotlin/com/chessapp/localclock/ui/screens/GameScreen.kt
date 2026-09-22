package com.chessapp.localclock.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.chessapp.engine.Color
import com.chessapp.localclock.ui.components.CapturedPiecesRow
import com.chessapp.localclock.ui.components.ChessBoard
import com.chessapp.localclock.ui.components.ClockDisplay
import com.chessapp.localclock.ui.components.GameOverDialog
import com.chessapp.localclock.ui.components.MoveHistoryList
import com.chessapp.localclock.ui.components.PromotionDialog
import com.chessapp.localclock.viewmodel.GameUiState
import com.chessapp.localclock.viewmodel.GameViewModel

/**
 * Tablet layout: the board pane (Black's bar, the board, White's bar) takes most of the
 * width and grows to fill all available height, so the board is as large as the screen
 * allows; the move list and game controls live in a narrower side panel.
 */
@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onBackToSetup: () -> Unit
) {
    val uiState = viewModel.uiState
    var showResignConfirm by remember { mutableStateOf(false) }
    var showDrawOfferConfirm by remember { mutableStateOf(false) }

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

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(3f)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Black sits opposite White at the table, so Black's bar is rotated 180° to face
            // them; this rotation is fixed to their seat and does not change with whose turn
            // it is (unlike the board's pieces, which do rotate each turn).
            PlayerBar(
                name = "Black",
                playerColor = Color.BLACK,
                uiState = uiState,
                facingRotationDegrees = 180f
            )

            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                ChessBoard(
                    uiState = uiState,
                    onSquareTapped = viewModel::onSquareTapped,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(Modifier.height(8.dp))
            if (uiState.isBotThinking) {
                Text(
                    "Computer is thinking…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))

            PlayerBar(
                name = "White",
                playerColor = Color.WHITE,
                uiState = uiState,
                facingRotationDegrees = 0f
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Text(
                "Moves",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            MoveHistoryList(
                moves = uiState.position.moveHistory,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { showDrawOfferConfirm = true },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isGameOver && uiState.botColor == null
                ) {
                    Text("Offer draw")
                }
                OutlinedButton(
                    onClick = { showResignConfirm = true },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isGameOver
                ) {
                    Text("Resign")
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onBackToSetup, modifier = Modifier.fillMaxWidth()) {
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
        val resigningColor = uiState.humanColor ?: uiState.position.sideToMove
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

    if (showDrawOfferConfirm) {
        val offeringColor = uiState.position.sideToMove
        val decidingColor = if (offeringColor == Color.WHITE) Color.BLACK else Color.WHITE
        val offeringName = if (offeringColor == Color.WHITE) "White" else "Black"
        val decidingName = if (decidingColor == Color.WHITE) "White" else "Black"
        AlertDialog(
            onDismissRequest = { showDrawOfferConfirm = false },
            title = { Text("Draw offered") },
            text = { Text("$offeringName offers a draw. $decidingName, do you accept?") },
            confirmButton = {
                TextButton(onClick = {
                    showDrawOfferConfirm = false
                    viewModel.agreeToDraw()
                }) { Text("Accept") }
            },
            dismissButton = {
                TextButton(onClick = { showDrawOfferConfirm = false }) { Text("Decline") }
            }
        )
    }
}

/**
 * One player's name, clock and captured pieces, positioned at their end of the board and
 * rotated by [facingRotationDegrees] so the whole bar reads right-side-up from their seat.
 * That rotation is fixed per player (Black's seat is always "up" from the app's own frame,
 * White's is always "down") and never changes with whose turn it is.
 */
@Composable
private fun PlayerBar(
    name: String,
    playerColor: Color,
    uiState: GameUiState,
    facingRotationDegrees: Float,
    modifier: Modifier = Modifier
) {
    val opponentColor = if (playerColor == Color.WHITE) Color.BLACK else Color.WHITE
    Row(
        modifier = modifier
            .fillMaxWidth()
            .rotate(facingRotationDegrees),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(name, style = MaterialTheme.typography.labelLarge)
            CapturedPiecesRow(captured = uiState.capturedPieces(opponentColor), color = opponentColor)
        }
        ClockDisplay(
            label = name,
            millisRemaining = uiState.clock.remaining(playerColor),
            isActive = uiState.clock.activeColor == playerColor && !uiState.isGameOver,
            isUnlimited = uiState.clock.isUnlimited,
            isFlagged = uiState.clock.flaggedColor == playerColor
        )
    }
}
