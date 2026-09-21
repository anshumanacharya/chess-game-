package com.chessapp.localclock.viewmodel

import com.chessapp.engine.ClockConfig
import com.chessapp.engine.ClockState
import com.chessapp.engine.Color
import com.chessapp.engine.GameState
import com.chessapp.engine.GameStatus
import com.chessapp.engine.Move
import com.chessapp.engine.MoveGenerator
import com.chessapp.engine.PieceType
import com.chessapp.engine.Square

enum class GameOverReason {
    CHECKMATE,
    STALEMATE,
    DRAW_FIFTY_MOVE,
    DRAW_REPETITION,
    DRAW_INSUFFICIENT_MATERIAL,
    WHITE_TIME_OUT,
    BLACK_TIME_OUT,
    WHITE_RESIGNED,
    BLACK_RESIGNED
}

/** A pawn reached the last rank: user must pick which piece it becomes before the move is applied. */
data class PendingPromotion(val from: Square, val to: Square, val options: List<Move>)

data class GameUiState(
    val position: GameState = GameState.newGame(),
    val clock: ClockState = ClockState.from(ClockConfig.UNLIMITED),
    val flipBoardEachTurn: Boolean = true,
    val selectedSquare: Square? = null,
    val pendingPromotion: PendingPromotion? = null,
    val gameOverReason: GameOverReason? = null
) {
    val status: GameStatus get() = MoveGenerator.status(position)
    val lastMove: Move? get() = position.moveHistory.lastOrNull()

    val legalMovesForSelected: List<Move>
        get() = selectedSquare?.let { MoveGenerator.legalMovesFrom(position, it) } ?: emptyList()

    val isGameOver: Boolean get() = gameOverReason != null

    /** null means the game ended in a draw. */
    val winner: Color?
        get() = when (gameOverReason) {
            GameOverReason.CHECKMATE -> position.sideToMove.opposite()
            GameOverReason.WHITE_TIME_OUT, GameOverReason.WHITE_RESIGNED -> Color.BLACK
            GameOverReason.BLACK_TIME_OUT, GameOverReason.BLACK_RESIGNED -> Color.WHITE
            else -> null
        }

    fun capturedPieces(color: Color): List<PieceType> {
        val startCounts = linkedMapOf(
            PieceType.QUEEN to 1,
            PieceType.ROOK to 2,
            PieceType.BISHOP to 2,
            PieceType.KNIGHT to 2,
            PieceType.PAWN to 8
        )
        val onBoard = position.board.piecesOf(color).groupingBy { it.second.type }.eachCount()
        val captured = mutableListOf<PieceType>()
        for ((type, startCount) in startCounts) {
            val remaining = onBoard[type] ?: 0
            repeat((startCount - remaining).coerceAtLeast(0)) { captured.add(type) }
        }
        return captured
    }
}
