package com.chessapp.localclock.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.chessapp.engine.Color as EngineColor
import com.chessapp.engine.GameState
import com.chessapp.engine.Move
import com.chessapp.engine.MoveGenerator
import com.chessapp.engine.Piece
import com.chessapp.engine.Square
import com.chessapp.localclock.ui.theme.BoardCaptureTarget
import com.chessapp.localclock.ui.theme.BoardCheck
import com.chessapp.localclock.ui.theme.BoardDarkSquare
import com.chessapp.localclock.ui.theme.BoardLastMove
import com.chessapp.localclock.ui.theme.BoardLegalTarget
import com.chessapp.localclock.ui.theme.BoardLightSquare
import com.chessapp.localclock.ui.theme.BoardSelected

/**
 * Renders the 8x8 board. In pass-and-play it's in a fixed orientation (White at the bottom,
 * Black at the top) so the device can lie flat on a table between the two players, with the
 * pieces (every piece, both colors) rotating 180° together whenever it's Black's turn so
 * whoever is about to move reads the whole board facing them.
 *
 * Against the bot there's only one human, sitting in one seat the whole game: playing as Black
 * permanently flips the board (like flipping the board on lichess/chess.com) so their own pieces
 * sit at the bottom, near them, instead of just rotating the piece artwork in place.
 */
@Composable
fun ChessBoard(
    position: GameState,
    selectedSquare: Square?,
    lastMove: Move?,
    kingInCheck: Square?,
    /** The human's color against the bot (fixes the seat and flips the board for Black); null in pass-and-play. */
    humanColor: EngineColor?,
    onSquareTapped: (Square) -> Unit,
    modifier: Modifier = Modifier
) {
    // Takes only what the board draws (not the whole GameUiState), so a clock tick — which
    // changes nothing here — lets Compose skip redrawing all 64 squares.
    val pos = position
    val legalTargets = remember(selectedSquare, pos) {
        selectedSquare?.let { from -> MoveGenerator.legalMovesFrom(pos, from).map { it.to }.toSet() } ?: emptySet()
    }
    val boardFlipped = humanColor == EngineColor.BLACK
    val pieceRotationDegrees = when {
        humanColor != null -> 0f
        pos.sideToMove == EngineColor.BLACK -> 180f
        else -> 0f
    }

    BoxWithConstraints(modifier = modifier.aspectRatio(1f)) {
        val squareSize = maxWidth / 8
        Column {
            for (displayRow in 0..7) {
                Row {
                    for (displayCol in 0..7) {
                        val file = if (boardFlipped) 7 - displayCol else displayCol
                        val rank = if (boardFlipped) displayRow else 7 - displayRow
                        val square = Square(file, rank)
                        val piece = pos.board.pieceAt(square)

                        SquareCell(
                            size = squareSize,
                            isLight = (file + rank) % 2 == 1,
                            piece = piece,
                            pieceRotationDegrees = pieceRotationDegrees,
                            isSelected = selectedSquare == square,
                            isLegalTarget = square in legalTargets,
                            isLastMove = lastMove?.let { it.from == square || it.to == square } == true,
                            isCheck = square == kingInCheck,
                            onClick = { onSquareTapped(square) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SquareCell(
    size: Dp,
    isLight: Boolean,
    piece: Piece?,
    pieceRotationDegrees: Float,
    isSelected: Boolean,
    isLegalTarget: Boolean,
    isLastMove: Boolean,
    isCheck: Boolean,
    onClick: () -> Unit
) {
    val baseColor = if (isLight) BoardLightSquare else BoardDarkSquare
    Box(
        modifier = Modifier
            .size(size)
            .background(baseColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isLastMove) {
            Box(Modifier.fillMaxSize().background(BoardLastMove.copy(alpha = 0.45f)))
        }
        if (isCheck) {
            // A radial red glow (lichess-style, mirrors web's .square-overlay.check) rather than a
            // flat wash, so it can't be mistaken for the flat capture tint when both show at once.
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val glow = BoardCheck.copy(alpha = 0.9f)
                        drawRect(
                            Brush.radialGradient(
                                0f to glow, 0.3f to glow, 0.78f to Color.Transparent,
                                center = center,
                                // CSS's default "farthest-corner" radius: half the diagonal.
                                // `this.size` (the draw area), not SquareCell's own `size: Dp`
                                // parameter, which shadows it here.
                                radius = this.size.minDimension * 0.7071f
                            )
                        )
                    }
            )
        }
        if (isSelected) {
            Box(Modifier.fillMaxSize().background(BoardSelected.copy(alpha = 0.55f)))
        }
        if (isLegalTarget) {
            if (piece == null) {
                Box(
                    Modifier
                        .size(size * 0.32f)
                        .clip(CircleShape)
                        .background(BoardLegalTarget.copy(alpha = 0.75f))
                )
            } else {
                // A soft red tint over the whole square marks a capturable enemy piece.
                Box(Modifier.fillMaxSize().background(BoardCaptureTarget.copy(alpha = 0.4f)))
            }
        }
        piece?.let {
            Image(
                painter = painterResource(id = pieceIconRes(it)),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize(0.86f)
                    .rotate(pieceRotationDegrees)
            )
        }
    }
}
