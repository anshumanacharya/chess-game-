package com.chessapp.localclock.viewmodel

import android.app.Application
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chessapp.engine.ClockConfig
import com.chessapp.engine.ClockState
import com.chessapp.engine.Color
import com.chessapp.engine.GameStatus
import com.chessapp.engine.Move
import com.chessapp.engine.PieceType
import com.chessapp.engine.Square
import com.chessapp.localclock.bot.AndroidConnectivityChecker
import com.chessapp.localclock.bot.BotMoveOrigin
import com.chessapp.localclock.bot.BotStrategy
import com.chessapp.localclock.bot.LocalBotSource
import com.chessapp.localclock.bot.RemoteBotSource
import com.chessapp.localclock.game.GameSource
import com.chessapp.localclock.game.LocalGameSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * [gameSource] defaults to on-device pass-and-play but is swappable — a future online mode
 * (e.g. backed by Lichess's Board API) plugs in as another [GameSource] without this class or
 * the UI it drives needing to change. `@JvmOverloads` keeps the single-arg (Application-only)
 * constructor Compose's `viewModel()` helper instantiates via reflection, the same way it already
 * finds the default [AndroidViewModel] constructor shape; a custom `ViewModelProvider.Factory`
 * would supply different sources later.
 *
 * [botStrategy] needs a [Context][android.content.Context] to run the bot's remote (WebView)
 * source, which is why this is an [AndroidViewModel] rather than a plain `ViewModel`.
 */
class GameViewModel @JvmOverloads constructor(
    application: Application,
    private val gameSource: GameSource = LocalGameSource(),
    private val botStrategy: BotStrategy = BotStrategy(
        local = LocalBotSource(),
        remote = RemoteBotSource(application),
        connectivityChecker = AndroidConnectivityChecker(application)
    )
) : AndroidViewModel(application) {

    var uiState by mutableStateOf(GameUiState())
        private set

    /**
     * Whether the game screen (rather than setup) should show. Lives here, not in the UI's own
     * `remember`ed state, so it survives the Activity being recreated (e.g. switching dark mode)
     * exactly as long as the game it describes does — the two can't disagree.
     */
    var isInGame by mutableStateOf(false)
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
        isInGame = true
        uiState = GameUiState(
            summary = PositionSummary(gameSource.newGame()),
            clock = ClockState.from(clockConfig).start(Color.WHITE),
            botColor = botColor
        )
        if (!clockConfig.isUnlimited) startTicker()
        maybeTriggerBotMove()
    }

    fun rematch() = startNewGame(lastClockConfig, lastBotColor)

    /** Back to setup: stops this game's clock and any pending bot move, which would otherwise
     *  keep running unseen behind the setup screen. */
    fun leaveGame() {
        tickerJob?.cancel()
        botMoveJob?.cancel()
        isInGame = false
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            // Monotonic, unlike System.currentTimeMillis(): a wall-clock adjustment (network
            // time sync, the user changing the time) can't add or remove time from a clock.
            var lastTick = SystemClock.elapsedRealtime()
            while (isActive) {
                delay(TICK_MS)
                val now = SystemClock.elapsedRealtime()
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

    /** Resume ticking after [pauseClock], if a game is on and has a real time control. Doesn't
     *  require an active clock side: the bot's clock is briefly paused while it fetches a move
     *  (see [maybeTriggerBotMove]), and a ticker left stopped then would freeze the clock once
     *  the bot's side is restarted. Ticking a paused clock is a no-op. */
    fun resumeClock() {
        if (isInGame && !uiState.isGameOver && !uiState.clock.isUnlimited) {
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
        val newSummary = PositionSummary(gameSource.applyMove(state.position, move))
        val nextColor = newSummary.position.sideToMove
        val newClock = state.clock.onMoveCompleted(movedColor, nextColor)

        uiState = state.copy(
            summary = newSummary,
            selectedSquare = null,
            pendingPromotion = null,
            clock = newClock
        )

        when (newSummary.status) {
            GameStatus.CHECKMATE -> endGame(GameOverReason.CHECKMATE)
            GameStatus.STALEMATE -> endGame(GameOverReason.STALEMATE)
            GameStatus.DRAW_FIFTY_MOVE -> endGame(GameOverReason.DRAW_FIFTY_MOVE)
            GameStatus.DRAW_REPETITION -> endGame(GameOverReason.DRAW_REPETITION)
            GameStatus.DRAW_INSUFFICIENT_MATERIAL -> endGame(GameOverReason.DRAW_INSUFFICIENT_MATERIAL)
            else -> maybeTriggerBotMove()
        }
    }

    /**
     * If it's the bot's turn, picks its move (online: the bot hosted on GitHub Pages; offline
     * or if that fails: the copy bundled with this app — see [BotStrategy]) after a short delay
     * so its reply doesn't feel instantaneous, then applies it exactly like a human move.
     * Re-checks [GameUiState.isBotTurn] after the delay/search in case the game ended (e.g. the
     * human resigned) while it was "thinking".
     *
     * The bot's clock runs during that fixed delay, but is paused while the move is actually
     * fetched: the online source can wait up to its timeout on a slow network (and loads the
     * whole engine on its first call), and charging the bot for that would make a timed game
     * depend on connection speed rather than play.
     */
    private fun maybeTriggerBotMove() {
        if (!uiState.isBotTurn) return
        botMoveJob?.cancel()
        uiState = uiState.copy(isBotThinking = true)
        botMoveJob = viewModelScope.launch {
            delay(BOT_MOVE_DELAY_MS)
            val position = uiState.position
            val botColor = position.sideToMove
            uiState = uiState.copy(clock = uiState.clock.pause())
            val result = try {
                botStrategy.chooseMove(position)
            } finally {
                // Hand the clock back even if this job was cancelled mid-fetch (resign, new game),
                // unless the game has since ended — a finished game's clock stays stopped.
                if (!uiState.isGameOver && uiState.position === position) {
                    uiState = uiState.copy(clock = uiState.clock.start(botColor))
                }
            }
            if (!uiState.isBotTurn || uiState.position !== position) return@launch
            uiState = uiState.copy(isBotThinking = false, lastBotMoveWasOffline = result.origin == BotMoveOrigin.LOCAL)
            if (result.move != null) applyMove(result.move)
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
        botStrategy.close()
    }

    companion object {
        private const val TICK_MS = 100L
        private const val BOT_MOVE_DELAY_MS = 500L
    }
}
