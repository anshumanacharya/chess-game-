package com.chessapp.localclock.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.saveable.rememberSaveable
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
    var showResignConfirm by rememberSaveable { mutableStateOf(false) }
    var showDrawOfferConfirm by rememberSaveable { mutableStateOf(false) }
    var sidePanelHidden by rememberSaveable { mutableStateOf(false) }

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

    // Side panel's slot: the original 1/4 column share plus its 24dp gap, as an explicit width so
    // it can animate to 0 and hand the whole row to the board.
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val panelSlotWidth: Dp = (maxWidth - 48.dp - 24.dp) / 4 + 24.dp
    val animatedSlotWidth by animateDpAsState(
        targetValue = if (sidePanelHidden) 0.dp else panelSlotWidth,
        label = "sidePanelSlot"
    )
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Whichever color sits "near" the human (bottom of the board) gets the bottom bar too,
        // matching the board flip in ChessBoard. In pass-and-play there's no single human seat,
        // so this defaults to the traditional Black-top/White-bottom layout; against the bot,
        // only the top bar (wherever the opponent ends up) rotates — there's just one real seat.
        val humanIsBlack = uiState.humanColor == Color.BLACK
        val topColor = if (humanIsBlack) Color.WHITE else Color.BLACK
        val bottomColor = if (humanIsBlack) Color.BLACK else Color.WHITE
        val topRotationDegrees = if (uiState.botColor == null) 180f else 0f

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PlayerBar(
                playerColor = topColor,
                uiState = uiState,
                facingRotationDegrees = topRotationDegrees
            )

            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                ChessBoard(
                    position = uiState.position,
                    selectedSquare = uiState.selectedSquare,
                    lastMove = uiState.summary.lastMove,
                    kingInCheck = uiState.summary.kingInCheck,
                    humanColor = uiState.humanColor,
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
            } else if (uiState.botColor != null && uiState.lastBotMoveWasOffline) {
                // The bot normally runs the latest version hosted on GitHub Pages; this only
                // shows when that wasn't reachable and it fell back to the copy bundled with
                // this app instead.
                Text(
                    "Offline — using built-in bot",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))

            PlayerBar(
                playerColor = bottomColor,
                uiState = uiState,
                facingRotationDegrees = 0f
            )
        }

        // Clipped slot whose width animates; the content keeps its full width (requiredWidth) so
        // it slides out of view instead of squashing.
        Box(
            modifier = Modifier
                .width(animatedSlotWidth)
                .fillMaxHeight()
                .clipToBounds()
        ) {
        Column(
            modifier = Modifier
                .requiredWidth(panelSlotWidth)
                .fillMaxHeight()
                .padding(start = 24.dp)
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
            OutlinedButton(
                onClick = viewModel::takeback,
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.canTakeback
            ) {
                Text("Takeback")
            }
            Spacer(Modifier.height(12.dp))
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
    }

    // A tab on the right screen edge (inside the 24dp outer padding, so it never covers the
    // panel) that stays put while the panel slides.
    Surface(
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .size(width = 24.dp, height = 64.dp)
            .semantics {
                contentDescription = if (sidePanelHidden) "Show side panel" else "Hide side panel"
            }
            .clickable(role = Role.Button) { sidePanelHidden = !sidePanelHidden },
        shape = RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                if (sidePanelHidden) "‹" else "›",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
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
 * One player's clock and captured pieces (with the classic +N material-advantage count),
 * positioned at their end of the board and rotated by [facingRotationDegrees] so the whole bar
 * reads right-side-up from their seat. No name label: position and the captured pieces' own
 * colors already say whose bar this is.
 */
@Composable
private fun PlayerBar(
    playerColor: Color,
    uiState: GameUiState,
    facingRotationDegrees: Float,
    modifier: Modifier = Modifier
) {
    val opponentColor = if (playerColor == Color.WHITE) Color.BLACK else Color.WHITE
    val ownCaptures = uiState.summary.capturedPieces(opponentColor) // pieces THIS player has captured
    val advantage = uiState.summary.materialAdvantage(playerColor)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .rotate(facingRotationDegrees),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CapturedPiecesRow(captured = ownCaptures, color = opponentColor, advantage = advantage)
        ClockDisplay(
            millisRemaining = uiState.clock.remaining(playerColor),
            isActive = uiState.clock.activeColor == playerColor && !uiState.isGameOver,
            isUnlimited = uiState.clock.isUnlimited,
            isFlagged = uiState.clock.flaggedColor == playerColor
        )
    }
}
