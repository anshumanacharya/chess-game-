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

    /** Piece types of `color` that are no longer on the board (i.e. captured by the opponent). */
    fun capturedPieces(color: Color): List<PieceType> {
        val startCounts = linkedMapOf(
            PieceType.QUEEN to 1,
            PieceType.ROOK to 2,
            PieceType.BISHOP to 2,
            PieceType.KNIGHT to 2,
            PieceType.PAWN to 8
        )
        val onBoard = state.board.piecesOf(color).groupingBy { it.second.type }.eachCount()
        val captured = mutableListOf<PieceType>()
        for ((type, startCount) in startCounts) {
            val remaining = onBoard[type] ?: 0
            repeat((startCount - remaining).coerceAtLeast(0)) { captured.add(type) }
        }
        return captured
    }

    fun reset() {
        state = GameState.newGame()
    }
}
