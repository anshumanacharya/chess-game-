package com.chessapp.localclock.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color as UiColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.chessapp.engine.Color as EngineColor
import com.chessapp.engine.GameStatus
import com.chessapp.engine.Piece
import com.chessapp.engine.Square
import com.chessapp.localclock.ui.theme.BoardCheck
import com.chessapp.localclock.ui.theme.BoardDarkSquare
import com.chessapp.localclock.ui.theme.BoardLastMove
import com.chessapp.localclock.ui.theme.BoardLegalTarget
import com.chessapp.localclock.ui.theme.BoardLightSquare
import com.chessapp.localclock.ui.theme.BoardSelected
import com.chessapp.localclock.viewmodel.GameUiState

/**
 * Renders the 8x8 board in a fixed orientation (White at the bottom, Black at the top) so
 * the device can lie flat on a table between the two players without flipping every move.
 * The piece glyphs (every piece, both colors) rotate 180° together whenever it's Black's turn,
 * so whoever is about to move sees the whole board — including the opponent's pieces — facing them.
 */
@Composable
fun ChessBoard(
    uiState: GameUiState,
    onSquareTapped: (Square) -> Unit,
    modifier: Modifier = Modifier
) {
    val pos = uiState.position
    val legalTargets = remember(uiState.selectedSquare, pos) {
        uiState.legalMovesForSelected.map { it.to }.toSet()
    }
    val kingInCheckSquare = if (uiState.status == GameStatus.CHECK || uiState.status == GameStatus.CHECKMATE) {
        pos.board.findKing(pos.sideToMove)
    } else null
    val pieceRotationDegrees = if (pos.sideToMove == EngineColor.BLACK) 180f else 0f

    BoxWithConstraints(modifier = modifier.aspectRatio(1f)) {
        val squareSize = maxWidth / 8
        Column {
            for (displayRow in 0..7) {
                Row {
                    for (displayCol in 0..7) {
                        val file = displayCol
                        val rank = 7 - displayRow
                        val square = Square(file, rank)
                        val piece = pos.board.pieceAt(square)

                        SquareCell(
                            size = squareSize,
                            isLight = (file + rank) % 2 == 1,
                            piece = piece,
                            pieceRotationDegrees = pieceRotationDegrees,
                            isSelected = uiState.selectedSquare == square,
                            isLegalTarget = square in legalTargets,
                            isLastMove = uiState.lastMove?.let { it.from == square || it.to == square } == true,
                            isCheck = square == kingInCheckSquare,
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
            Box(Modifier.fillMaxSize().background(BoardCheck.copy(alpha = 0.55f)))
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
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(size * 0.06f)
                        .border(width = size * 0.06f, color = BoardLegalTarget.copy(alpha = 0.85f), shape = CircleShape)
                )
            }
        }
        piece?.let {
            val density = LocalDensity.current
            val (fontSizeSp, outlineWidthPx) = with(density) {
                val fontSizePx = size.toPx() * 0.72f
                fontSizePx.toSp() to fontSizePx * 0.09f
            }
            // The Unicode "white piece" glyphs (♔♕♖…) are hollow outlines with almost no
            // fillable body, so a white-fill/black-stroke treatment on them just looks thin
            // and mostly black. Use the solid "black piece" glyph shapes for both colors here
            // and let color/stroke do the coloring instead.
            val glyph = pieceGlyph(it.type, EngineColor.BLACK)
            Box(contentAlignment = Alignment.Center, modifier = Modifier.rotate(pieceRotationDegrees)) {
                if (it.color == EngineColor.WHITE) {
                    Text(
                        text = glyph,
                        fontSize = fontSizeSp,
                        color = UiColor.Black,
                        style = TextStyle(drawStyle = Stroke(width = outlineWidthPx))
                    )
                    Text(
                        text = glyph,
                        fontSize = fontSizeSp,
                        color = UiColor.White
                    )
                } else {
                    Text(
                        text = glyph,
                        fontSize = fontSizeSp,
                        color = UiColor(0xFF141414)
                    )
                }
            }
        }
    }
}
