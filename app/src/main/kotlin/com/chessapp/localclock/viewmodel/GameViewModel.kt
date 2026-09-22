package com.chessapp.localclock.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chessapp.engine.ChessBot
import com.chessapp.engine.ClockConfig
import com.chessapp.engine.ClockState
import com.chessapp.engine.Color
import com.chessapp.engine.GameStatus
import com.chessapp.engine.Move
import com.chessapp.engine.MoveGenerator
import com.chessapp.engine.PieceType
import com.chessapp.engine.Square
import com.chessapp.localclock.game.GameSource
import com.chessapp.localclock.game.LocalGameSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * [gameSource] defaults to on-device pass-and-play but is swappable — a future online mode
 * (e.g. backed by Lichess's Board API) plugs in as another [GameSource] without this class or
 * the UI it drives needing to change. `@JvmOverloads` keeps the zero-arg constructor Compose's
 * `viewModel()` helper instantiates via reflection; a custom `ViewModelProvider.Factory` would
 * supply a different source later.
 */
class GameViewModel @JvmOverloads constructor(
    private val gameSource: GameSource = LocalGameSource(),
    private val bot: ChessBot = ChessBot()
) : ViewModel() {

    var uiState by mutableStateOf(GameUiState())
        private set

    private var tickerJob: Job? = null
    private var botMoveJob: Job? = null
    private var lastClockConfig: ClockConfig = ClockConfig.UNLIMITED
    private var lastBotColor: Color? = null

    fun startNewGame(clockConfig: ClockConfig, botColor: Color? = null) {
        tickerJob?.cancel()
        botMoveJob?.cancel()
        lastClockConfig = clockConfig
        lastBotColor = botColor
        uiState = GameUiState(
            position = gameSource.newGame(),
            clock = ClockState.from(clockConfig).start(Color.WHITE),
            botColor = botColor
        )
        if (!clockConfig.isUnlimited) startTicker()
        maybeTriggerBotMove()
    }

    fun rematch() = startNewGame(lastClockConfig, lastBotColor)

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            var lastTick = System.currentTimeMillis()
            while (isActive) {
                delay(TICK_MS)
                val now = System.currentTimeMillis()
                val elapsed = now - lastTick
                lastTick = now
                if (uiState.isGameOver) break
                val newClock = uiState.clock.tick(elapsed)
                uiState = uiState.copy(clock = newClock)
                val flagged = newClock.flaggedColor
                if (flagged != null) {
                    endGame(if (flagged == Color.WHITE) GameOverReason.WHITE_TIME_OUT else GameOverReason.BLACK_TIME_OUT)
                    break
                }
            }
        }
    }

    /** Stop the clock ticking without ending the game (e.g. app backgrounded). */
    fun pauseClock() {
        tickerJob?.cancel()
    }

    /** Resume ticking after [pauseClock], if the game is still on and has a real time control. */
    fun resumeClock() {
        if (!uiState.isGameOver && !uiState.clock.isUnlimited && uiState.clock.activeColor != null) {
            startTicker()
        }
    }

    fun onSquareTapped(square: Square) {
        if (uiState.isGameOver || uiState.isBotTurn) return
        val state = uiState
        val pos = state.position
        val pieceAtSquare = pos.board.pieceAt(square)

        val selected = state.selectedSquare
        if (selected == null) {
            if (pieceAtSquare != null && pieceAtSquare.color == pos.sideToMove) {
                uiState = state.copy(selectedSquare = square)
            }
            return
        }

        if (selected == square) {
            uiState = state.copy(selectedSquare = null)
            return
        }

        val movesToSquare = gameSource.legalMovesFrom(pos, selected).filter { it.to == square }
        when {
            movesToSquare.isEmpty() -> {
                uiState = if (pieceAtSquare != null && pieceAtSquare.color == pos.sideToMove) {
                    state.copy(selectedSquare = square)
                } else {
                    state.copy(selectedSquare = null)
                }
            }
            movesToSquare.size > 1 -> {
                uiState = state.copy(pendingPromotion = PendingPromotion(selected, square, movesToSquare))
            }
            else -> applyMove(movesToSquare.first())
        }
    }

    fun choosePromotion(pieceType: PieceType) {
        val pending = uiState.pendingPromotion ?: return
        val move = pending.options.firstOrNull { it.promotion == pieceType } ?: return
        uiState = uiState.copy(pendingPromotion = null)
        applyMove(move)
    }

    fun cancelSelection() {
        uiState = uiState.copy(selectedSquare = null, pendingPromotion = null)
    }

    private fun applyMove(move: Move) {
        val state = uiState
        val movedColor = state.position.sideToMove
        val newPosition = gameSource.applyMove(state.position, move)
        val nextColor = newPosition.sideToMove
        val newClock = state.clock.onMoveCompleted(movedColor, nextColor)

        uiState = state.copy(
            position = newPosition,
            selectedSquare = null,
            pendingPromotion = null,
            clock = newClock
        )

        when (MoveGenerator.status(newPosition)) {
            GameStatus.CHECKMATE -> endGame(GameOverReason.CHECKMATE)
            GameStatus.STALEMATE -> endGame(GameOverReason.STALEMATE)
            GameStatus.DRAW_FIFTY_MOVE -> endGame(GameOverReason.DRAW_FIFTY_MOVE)
            GameStatus.DRAW_REPETITION -> endGame(GameOverReason.DRAW_REPETITION)
            GameStatus.DRAW_INSUFFICIENT_MATERIAL -> endGame(GameOverReason.DRAW_INSUFFICIENT_MATERIAL)
            else -> maybeTriggerBotMove()
        }
    }

    /**
     * If it's the bot's turn, picks its move on a background dispatcher (the search is cheap
     * but still real CPU work) after a short delay so its reply doesn't feel instantaneous,
     * then applies it exactly like a human move. Re-checks [GameUiState.isBotTurn] after the
     * delay/search in case the game ended (e.g. the human resigned) while it was "thinking".
     */
    private fun maybeTriggerBotMove() {
        if (!uiState.isBotTurn) return
        botMoveJob?.cancel()
        uiState = uiState.copy(isBotThinking = true)
        botMoveJob = viewModelScope.launch {
            delay(BOT_MOVE_DELAY_MS)
            val position = uiState.position
            val move = withContext(Dispatchers.Default) { bot.chooseMove(position) }
            if (!uiState.isBotTurn || uiState.position !== position) return@launch
            uiState = uiState.copy(isBotThinking = false)
            if (move != null) applyMove(move)
        }
    }

    fun resign(color: Color) {
        endGame(if (color == Color.WHITE) GameOverReason.WHITE_RESIGNED else GameOverReason.BLACK_RESIGNED)
    }

    fun agreeToDraw() {
        endGame(GameOverReason.DRAW_AGREED)
    }

    private fun endGame(reason: GameOverReason) {
        tickerJob?.cancel()
        botMoveJob?.cancel()
        uiState = uiState.copy(
            gameOverReason = reason,
            selectedSquare = null,
            pendingPromotion = null,
            isBotThinking = false
        )
    }

    override fun onCleared() {
        tickerJob?.cancel()
        botMoveJob?.cancel()
    }

    companion object {
        private const val TICK_MS = 100L
        private const val BOT_MOVE_DELAY_MS = 500L
    }
}
