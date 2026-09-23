package com.chessapp.engine

/**
 * Mutable convenience wrapper around [GameState] + [MoveGenerator] for UI consumption.
 * Typical usage: call [legalMovesFrom] for the tapped square, let the user pick one of the
 * returned [Move]s (prompting for a promotion piece if several share the same `to` square),
 * then pass that exact move to [makeMove].
 */
class ChessGame(initialState: GameState = GameState.newGame()) {

    var state: GameState = initialState
        private set

    val sideToMove: Color get() = state.sideToMove
    val moveHistory: List<Move> get() = state.moveHistory

    fun legalMovesFrom(square: Square): List<Move> = MoveGenerator.legalMovesFrom(state, square)

    fun allLegalMoves(): List<Move> = MoveGenerator.legalMoves(state)

    fun status(): GameStatus = MoveGenerator.status(state)

    fun isInCheck(color: Color = state.sideToMove): Boolean = MoveGenerator.isInCheck(state, color)

    fun makeMove(move: Move): Boolean {
        if (move !in MoveGenerator.legalMoves(state)) return false
        state = MoveGenerator.applyMove(state, move)
        return true
    }

    /** Pieces of `color` captured by the opponent, most valuable first. */
    fun capturedPieces(color: Color): List<PieceType> = state.capturedPieces(color)

    /** `color`'s material minus the opponent's, from the pieces on the board. */
    fun materialAdvantage(color: Color): Int = state.materialAdvantage(color)

    fun reset() {
        state = GameState.newGame()
    }
}
