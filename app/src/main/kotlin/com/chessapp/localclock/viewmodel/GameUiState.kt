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
import com.chessapp.engine.capturedPieces
import com.chessapp.engine.materialAdvantage

enum class GameOverReason {
    CHECKMATE,
    STALEMATE,
    DRAW_FIFTY_MOVE,
    DRAW_REPETITION,
    DRAW_INSUFFICIENT_MATERIAL,
    WHITE_TIME_OUT,
    BLACK_TIME_OUT,
    WHITE_RESIGNED,
    BLACK_RESIGNED,
    DRAW_AGREED
}

/** A pawn reached the last rank: user must pick which piece it becomes before the move is applied. */
data class PendingPromotion(val from: Square, val to: Square, val options: List<Move>)

/**
 * Everything the UI derives from a position alone — status, last move, the king in check,
 * captured pieces and material — computed once when the position changes. [GameUiState] is
 * copied ten times a second by the clock ticker; keeping these in one object that each copy
 * shares (instead of getters on [GameUiState]) means a tick recomputes none of them, and lets
 * composables that only read the position skip recomposing on a tick at all.
 *
 * These read pure, already-committed board state to drive display, so they call the engine
 * directly regardless of which GameSource produced [position] — only GameViewModel's actual move
 * submission path goes through GameSource, since that's the part a future online source needs
 * to intercept.
 */
class PositionSummary(val position: GameState) {
    val status: GameStatus = MoveGenerator.status(position)
    val lastMove: Move? = position.moveHistory.lastOrNull()

    /** The side to move's king, when it's in check (or mated); null otherwise. */
    val kingInCheck: Square? =
        if (status == GameStatus.CHECK || status == GameStatus.CHECKMATE) position.board.findKing(position.sideToMove) else null

    private val captured: Map<Color, List<PieceType>> = Color.entries.associateWith { position.capturedPieces(it) }
    private val whiteMaterialAdvantage: Int = position.materialAdvantage(Color.WHITE)

    /** Pieces of [color] captured by the opponent, most valuable first. */
    fun capturedPieces(color: Color): List<PieceType> = captured.getValue(color)

    /** [color]'s material minus the opponent's (the "+N" count). */
    fun materialAdvantage(color: Color): Int = if (color == Color.WHITE) whiteMaterialAdvantage else -whiteMaterialAdvantage
}

data class GameUiState(
    val summary: PositionSummary = PositionSummary(GameState.newGame()),
    val clock: ClockState = ClockState.from(ClockConfig.UNLIMITED),
    val selectedSquare: Square? = null,
    val pendingPromotion: PendingPromotion? = null,
    val gameOverReason: GameOverReason? = null,
    /** null means pass-and-play (both sides human); otherwise the color the bot plays. */
    val botColor: Color? = null,
    val isBotThinking: Boolean = false,
    /** Whether the bot's most recent move came from the bundled offline copy rather than the
     *  one hosted on GitHub Pages — because the device is offline, or the online one failed.
     *  Meaningless (and unused) outside a vs-bot game. */
    val lastBotMoveWasOffline: Boolean = false
) {
    val position: GameState get() = summary.position
    val status: GameStatus get() = summary.status

    val isGameOver: Boolean get() = gameOverReason != null

    /** The human's color when playing against the bot; null in pass-and-play. */
    val humanColor: Color? get() = botColor?.opposite()

    val isBotTurn: Boolean get() = botColor != null && botColor == position.sideToMove && !isGameOver

    /** null means the game ended in a draw. */
    val winner: Color?
        get() = when (gameOverReason) {
            GameOverReason.CHECKMATE -> position.sideToMove.opposite()
            GameOverReason.WHITE_TIME_OUT, GameOverReason.WHITE_RESIGNED -> Color.BLACK
            GameOverReason.BLACK_TIME_OUT, GameOverReason.BLACK_RESIGNED -> Color.WHITE
            else -> null
        }
}
