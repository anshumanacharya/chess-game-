package com.chessapp.localclock.bot

import com.chessapp.engine.GameState
import com.chessapp.engine.Move

enum class BotMoveOrigin { LOCAL, REMOTE }

data class BotMoveResult(val move: Move?, val origin: BotMoveOrigin)

/**
 * Picks which [BotSource] computes the bot's next move: [remote] when the device currently has a
 * working internet connection, [local] otherwise — checked fresh via [connectivityChecker] before
 * every single move, so a connectivity change mid-game takes effect on the very next bot move
 * without needing to restart the game. If [remote] is configured but fails to produce a move for
 * any reason (page load failure, timeout, no legal move found), this transparently falls back to
 * [local] for that same move rather than leaving the bot unable to move.
 */
class BotStrategy(
    private val local: BotSource,
    private val remote: BotSource? = null,
    private val connectivityChecker: ConnectivityChecker? = null
) {
    suspend fun chooseMove(state: GameState): BotMoveResult {
        if (remote != null && connectivityChecker?.isOnline() == true) {
            val move = remote.chooseMove(state)
            if (move != null) return BotMoveResult(move, BotMoveOrigin.REMOTE)
        }
        return BotMoveResult(local.chooseMove(state), BotMoveOrigin.LOCAL)
    }

    /** Releases both sources; call once the owner (the game's ViewModel) is done with them. */
    fun close() {
        local.close()
        remote?.close()
    }
}
