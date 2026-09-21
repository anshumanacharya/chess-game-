package com.chessapp.localclock.game

import com.chessapp.engine.GameState
import com.chessapp.engine.Move
import com.chessapp.engine.MoveGenerator
import com.chessapp.engine.Square

/**
 * Where the authoritative chess position comes from and how moves get committed to it.
 *
 * [LocalGameSource] is today's synchronous, on-device pass-and-play: legality is checked and
 * moves are applied immediately, on the same device. A future network-backed source (e.g. one
 * built on Lichess's Board API) would implement the same interface instead — sending a
 * submitted move out over the network and only reflecting it (or an opponent's incoming move)
 * once the server confirms it — so [com.chessapp.localclock.viewmodel.GameViewModel] and the UI
 * it drives don't need to know or care which kind of game they're playing.
 */
interface GameSource {
    /** A fresh starting position for a new game from this source. */
    fun newGame(): GameState

    /** Legal moves for the piece on [from], used to decide what a tap on a destination means. */
    fun legalMovesFrom(state: GameState, from: Square): List<Move>

    /**
     * Commits [move] and returns the resulting state. For a local source this is immediate and
     * always succeeds for a move drawn from [legalMovesFrom]; a network source may instead need
     * to await server confirmation before this returns, or reject a move that raced with one
     * from the opponent.
     */
    fun applyMove(state: GameState, move: Move): GameState
}

/** Synchronous on-device pass-and-play: both players share one device, one source of truth. */
class LocalGameSource : GameSource {
    override fun newGame(): GameState = GameState.newGame()

    override fun legalMovesFrom(state: GameState, from: Square): List<Move> =
        MoveGenerator.legalMovesFrom(state, from)

    override fun applyMove(state: GameState, move: Move): GameState =
        MoveGenerator.applyMove(state, move)
}
