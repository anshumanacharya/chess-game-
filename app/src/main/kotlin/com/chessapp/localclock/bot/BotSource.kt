package com.chessapp.localclock.bot

import com.chessapp.bot.ChessBot
import com.chessapp.engine.GameState
import com.chessapp.engine.Move
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Where the bot's move for a given position comes from. Mirrors [com.chessapp.localclock.game.GameSource]:
 * a small seam so [BotStrategy] can pick between an offline-always-available implementation and
 * one that depends on the network, without the rest of the app needing to know which is active.
 */
interface BotSource {
    /**
     * The bot's move for [state], or null if it couldn't produce one — either because the side
     * to move has no legal move, or (for a source that can fail, like a network-backed one) it
     * wasn't able to compute a move at all. Callers that need to tell those two cases apart
     * should only call this when they've already confirmed a legal move exists.
     */
    suspend fun chooseMove(state: GameState): Move?

    /** Releases anything this source holds onto (e.g. a WebView). Call from the main thread. */
    fun close() {}
}

/** Always available, no network required: runs the bot bundled into this build at compile time. */
class LocalBotSource(private val bot: ChessBot = ChessBot()) : BotSource {
    override suspend fun chooseMove(state: GameState): Move? =
        withContext(Dispatchers.Default) { bot.chooseMove(state) }
}
