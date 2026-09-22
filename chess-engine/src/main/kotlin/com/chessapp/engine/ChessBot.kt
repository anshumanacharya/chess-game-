package com.chessapp.engine

import kotlin.random.Random

/**
 * A deliberately weak move-selection heuristic aimed at roughly club-beginner strength
 * (~1000 Elo). There was no rating engine or rating pool available to calibrate this against
 * a real number, so treat "1000 Elo" as a design target, not a verified rating.
 *
 * The shape of it: a two-ply material-only search (the bot's move, then the opponent's best
 * material-grabbing reply), so it doesn't hang pieces for free and finds a mate in one when
 * available — blurred with random evaluation noise so it isn't always objectively best, plus
 * a small independent chance of playing an outright random legal move to simulate a genuine
 * blunder (an oversight noise alone rarely produces).
 */
class ChessBot(
    private val blunderProbability: Double = 0.08,
    private val noiseCentipawns: Int = 150,
    private val random: Random = Random.Default
) {
    /** The bot's chosen move, or null if the side to move has no legal move (game already over). */
    fun chooseMove(state: GameState): Move? {
        val legalMoves = MoveGenerator.legalMoves(state)
        if (legalMoves.isEmpty()) return null

        if (random.nextDouble() < blunderProbability) {
            return legalMoves.random(random)
        }

        val botColor = state.sideToMove
        var bestMove = legalMoves.first()
        var bestScore = Int.MIN_VALUE
        for (move in legalMoves) {
            val afterBotMove = MoveGenerator.applyMove(state, move)
            val score = evaluateAfterBotMove(afterBotMove, botColor) + noise()
            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }
        }
        return bestMove
    }

    private fun noise(): Int =
        if (noiseCentipawns <= 0) 0 else random.nextInt(-noiseCentipawns, noiseCentipawns + 1)

    /**
     * Score of the position right after the bot's candidate move, from the bot's own
     * perspective, looking one more ply ahead at the opponent's best material-grabbing reply —
     * enough to notice a hanging piece or a forced mate without a full deep search.
     */
    private fun evaluateAfterBotMove(afterBotMove: GameState, botColor: Color): Int {
        val status = MoveGenerator.status(afterBotMove)
        if (status == GameStatus.CHECKMATE) return MATE_SCORE
        if (status == GameStatus.STALEMATE || status == GameStatus.DRAW_INSUFFICIENT_MATERIAL) return 0

        val opponentReplies = MoveGenerator.legalMoves(afterBotMove)
        if (opponentReplies.isEmpty()) return materialScore(afterBotMove.board, botColor)

        var worstForBot = Int.MAX_VALUE
        for (reply in opponentReplies) {
            val afterReply = MoveGenerator.applyMove(afterBotMove, reply)
            val replyIsMate = MoveGenerator.isInCheck(afterReply, botColor) &&
                MoveGenerator.legalMoves(afterReply).isEmpty()
            val score = if (replyIsMate) -MATE_SCORE else materialScore(afterReply.board, botColor)
            if (score < worstForBot) worstForBot = score
        }
        return worstForBot
    }

    private fun materialScore(board: Board, color: Color): Int {
        var score = 0
        for ((_, piece) in board.allPieces()) {
            val value = pieceValue(piece.type)
            score += if (piece.color == color) value else -value
        }
        return score
    }

    private fun pieceValue(type: PieceType): Int = when (type) {
        PieceType.PAWN -> 100
        PieceType.KNIGHT -> 300
        PieceType.BISHOP -> 300
        PieceType.ROOK -> 500
        PieceType.QUEEN -> 900
        PieceType.KING -> 0
    }

    companion object {
        private const val MATE_SCORE = 100_000
    }
}
