package com.chessapp.engine

/**
 * Immutable snapshot of a chess position. Making a move produces a new [GameState]
 * rather than mutating this one, so the UI layer can keep history / support undo trivially.
 *
 * [positionCounts] tracks how many times each position (see [MoveGenerator.positionKey])
 * has occurred, for threefold-repetition detection.
 */
data class GameState(
    val board: Board,
    val sideToMove: Color,
    val castlingRights: CastlingRights,
    val enPassantTarget: Square?,
    val halfMoveClock: Int,
    val fullMoveNumber: Int,
    val moveHistory: List<Move> = emptyList(),
    val positionCounts: Map<String, Int> = emptyMap()
) {
    companion object {
        fun newGame(): GameState {
            val start = GameState(
                board = Board.initialPosition(),
                sideToMove = Color.WHITE,
                castlingRights = CastlingRights(),
                enPassantTarget = null,
                halfMoveClock = 0,
                fullMoveNumber = 1
            )
            // The starting position is its own first occurrence for threefold repetition.
            return start.copy(positionCounts = mapOf(MoveGenerator.positionKey(start) to 1))
        }
    }
}
