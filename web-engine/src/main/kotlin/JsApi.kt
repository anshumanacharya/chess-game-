@file:OptIn(ExperimentalJsExport::class)

// Deliberately no package: exported classes then hang directly off the compiled bundle's
// global (e.g. `ChessEngine.JsGame`) instead of `ChessEngine.com.chessapp.web.JsGame`.

import com.chessapp.engine.ChessBot
import com.chessapp.engine.Color
import com.chessapp.engine.ClockConfig
import com.chessapp.engine.ClockState
import com.chessapp.engine.GameState
import com.chessapp.engine.GameStatus
import com.chessapp.engine.Move
import com.chessapp.engine.MoveGenerator
import com.chessapp.engine.PieceType
import com.chessapp.engine.Square

/**
 * A small, JS-ergonomic facade over the shared chess-engine rules and clock, used only by the
 * web build. Everything here is a thin translation layer (primitives and simple exported
 * classes in, primitives and simple exported classes out) — the actual chess rules and clock
 * behavior all come from the exact same source files the Android app compiles.
 */

@JsExport
class JsSquare(val file: Int, val rank: Int, val pieceType: String?, val pieceColor: String?)

@JsExport
class JsMove(
    val fromFile: Int,
    val fromRank: Int,
    val toFile: Int,
    val toRank: Int,
    val promotion: String?,
    val isCapture: Boolean,
    val flag: String,
    val algebraic: String
)

private fun colorFromString(s: String): Color = if (s == "white") Color.WHITE else Color.BLACK
private fun colorToString(c: Color): String = if (c == Color.WHITE) "white" else "black"

private fun pieceTypeFromString(s: String): PieceType = when (s) {
    "queen" -> PieceType.QUEEN
    "rook" -> PieceType.ROOK
    "bishop" -> PieceType.BISHOP
    "knight" -> PieceType.KNIGHT
    "pawn" -> PieceType.PAWN
    "king" -> PieceType.KING
    else -> throw IllegalArgumentException("Unknown piece type: $s")
}

private fun pieceTypeToString(t: PieceType): String = t.name.lowercase()

private fun statusToString(s: GameStatus): String = when (s) {
    GameStatus.ONGOING -> "ongoing"
    GameStatus.CHECK -> "check"
    GameStatus.CHECKMATE -> "checkmate"
    GameStatus.STALEMATE -> "stalemate"
    GameStatus.DRAW_FIFTY_MOVE -> "draw_fifty_move"
    GameStatus.DRAW_REPETITION -> "draw_repetition"
    GameStatus.DRAW_INSUFFICIENT_MATERIAL -> "draw_insufficient_material"
}

private fun Move.toJsMove(): JsMove = JsMove(
    fromFile = from.file,
    fromRank = from.rank,
    toFile = to.file,
    toRank = to.rank,
    promotion = promotion?.let(::pieceTypeToString),
    isCapture = isCapture,
    flag = flag.name,
    algebraic = toShortAlgebraic()
)

@JsExport
class JsGame {
    private var state: GameState = GameState.newGame()
    private val bot = ChessBot()
    private var botColor: Color? = null

    fun reset() {
        state = GameState.newGame()
        botColor = null
    }

    /** Sets which side (if any) the bot plays; pass null for a pass-and-play game. */
    fun setBot(color: String?) {
        botColor = color?.let(::colorFromString)
    }

    fun hasBot(): Boolean = botColor != null

    fun isBotTurn(): Boolean = botColor != null && botColor == state.sideToMove

    /**
     * Picks and applies the bot's move for the current position, returning it, or null if it
     * isn't the bot's turn or the game is already over (no legal move).
     */
    fun playBotMove(): JsMove? {
        if (!isBotTurn()) return null
        val move = bot.chooseMove(state) ?: return null
        state = MoveGenerator.applyMove(state, move)
        return move.toJsMove()
    }

    fun sideToMove(): String = colorToString(state.sideToMove)

    fun status(): String = statusToString(MoveGenerator.status(state))

    fun isInCheck(): Boolean = MoveGenerator.isInCheck(state, state.sideToMove)

    fun boardSquares(): Array<JsSquare> {
        val squares = ArrayList<JsSquare>(64)
        for (file in 0..7) {
            for (rank in 0..7) {
                val piece = state.board.pieceAt(Square(file, rank))
                squares.add(
                    JsSquare(
                        file,
                        rank,
                        piece?.let { pieceTypeToString(it.type) },
                        piece?.let { colorToString(it.color) }
                    )
                )
            }
        }
        return squares.toTypedArray()
    }

    fun legalMovesFrom(file: Int, rank: Int): Array<JsMove> =
        MoveGenerator.legalMovesFrom(state, Square(file, rank)).map { it.toJsMove() }.toTypedArray()

    /** Finds and applies the legal move matching these exact squares (and promotion, if any). */
    fun applyMoveExact(fromFile: Int, fromRank: Int, toFile: Int, toRank: Int, promotion: String?): Boolean {
        val from = Square(fromFile, fromRank)
        val to = Square(toFile, toRank)
        val promoType = promotion?.let(::pieceTypeFromString)
        val move = MoveGenerator.legalMovesFrom(state, from)
            .firstOrNull { it.to == to && it.promotion == promoType } ?: return false
        state = MoveGenerator.applyMove(state, move)
        return true
    }

    fun lastMoveAlgebraic(): String? = state.moveHistory.lastOrNull()?.toShortAlgebraic()

    /** The full last move (from/to squares included), for highlighting it on the board. */
    fun lastMove(): JsMove? = state.moveHistory.lastOrNull()?.toJsMove()

    fun sideToMoveKingSquareIfInCheck(): JsSquare? {
        if (!isInCheck()) return null
        val square = state.board.findKing(state.sideToMove)
        return JsSquare(square.file, square.rank, "king", colorToString(state.sideToMove))
    }

    fun capturedPieces(color: String): Array<String> {
        val target = colorFromString(color)
        val startCounts = linkedMapOf("queen" to 1, "rook" to 2, "bishop" to 2, "knight" to 2, "pawn" to 8)
        val onBoard = HashMap<String, Int>()
        for (file in 0..7) {
            for (rank in 0..7) {
                val piece = state.board.pieceAt(Square(file, rank))
                if (piece != null && piece.color == target && piece.type != PieceType.KING) {
                    val key = pieceTypeToString(piece.type)
                    onBoard[key] = (onBoard[key] ?: 0) + 1
                }
            }
        }
        val captured = ArrayList<String>()
        for ((type, startCount) in startCounts) {
            val remaining = onBoard[type] ?: 0
            repeat((startCount - remaining).coerceAtLeast(0)) { captured.add(type) }
        }
        return captured.toTypedArray()
    }
}

@JsExport
class JsClock(initialMinutes: Int, incrementSeconds: Int, unlimited: Boolean) {
    private var clock: ClockState = ClockState.from(
        if (unlimited) ClockConfig.UNLIMITED else ClockConfig.preset(initialMinutes, incrementSeconds)
    )

    fun start(color: String) {
        clock = clock.start(colorFromString(color))
    }

    fun pause() {
        clock = clock.pause()
    }

    fun tick(elapsedMillis: Double) {
        clock = clock.tick(elapsedMillis.toLong())
    }

    fun onMoveCompleted(movedColor: String, nextColor: String) {
        clock = clock.onMoveCompleted(colorFromString(movedColor), colorFromString(nextColor))
    }

    fun remainingMillis(color: String): Double = clock.remaining(colorFromString(color)).toDouble()

    fun isUnlimited(): Boolean = clock.isUnlimited

    fun activeColor(): String? = clock.activeColor?.let(::colorToString)

    fun flaggedColor(): String? = clock.flaggedColor?.let(::colorToString)
}
