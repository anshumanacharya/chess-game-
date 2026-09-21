package com.chessapp.engine

/**
 * Pure functions for legal chess move generation, check/checkmate/stalemate detection,
 * and applying moves to produce new [GameState] snapshots.
 */
object MoveGenerator {

    private val KNIGHT_OFFSETS = listOf(1 to 2, 2 to 1, 2 to -1, 1 to -2, -1 to -2, -2 to -1, -2 to 1, -1 to 2)
    private val KING_OFFSETS = listOf(1 to 0, 1 to 1, 0 to 1, -1 to 1, -1 to 0, -1 to -1, 0 to -1, 1 to -1)
    private val BISHOP_DIRS = listOf(1 to 1, 1 to -1, -1 to 1, -1 to -1)
    private val ROOK_DIRS = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
    private val QUEEN_DIRS = BISHOP_DIRS + ROOK_DIRS

    fun isSquareAttacked(board: Board, square: Square, byColor: Color): Boolean {
        // Pawn attacks: a `byColor` pawn attacks `square` from one rank behind it (relative to its own advance direction).
        val pawnRankOffset = if (byColor == Color.WHITE) -1 else 1
        for (df in intArrayOf(-1, 1)) {
            val s = Square.of(square.file + df, square.rank + pawnRankOffset) ?: continue
            val p = board.pieceAt(s)
            if (p != null && p.color == byColor && p.type == PieceType.PAWN) return true
        }
        for ((df, dr) in KNIGHT_OFFSETS) {
            val s = Square.of(square.file + df, square.rank + dr) ?: continue
            val p = board.pieceAt(s)
            if (p != null && p.color == byColor && p.type == PieceType.KNIGHT) return true
        }
        for ((df, dr) in KING_OFFSETS) {
            val s = Square.of(square.file + df, square.rank + dr) ?: continue
            val p = board.pieceAt(s)
            if (p != null && p.color == byColor && p.type == PieceType.KING) return true
        }
        for ((df, dr) in BISHOP_DIRS) {
            var f = square.file + df
            var r = square.rank + dr
            while (true) {
                val s = Square.of(f, r) ?: break
                val p = board.pieceAt(s)
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.BISHOP || p.type == PieceType.QUEEN)) return true
                    break
                }
                f += df; r += dr
            }
        }
        for ((df, dr) in ROOK_DIRS) {
            var f = square.file + df
            var r = square.rank + dr
            while (true) {
                val s = Square.of(f, r) ?: break
                val p = board.pieceAt(s)
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.ROOK || p.type == PieceType.QUEEN)) return true
                    break
                }
                f += df; r += dr
            }
        }
        return false
    }

    fun isInCheck(state: GameState, color: Color): Boolean {
        val kingSquare = state.board.findKing(color)
        return isSquareAttacked(state.board, kingSquare, color.opposite())
    }

    /** All moves for the side to move, ignoring whether the move leaves that side's own king in check. */
    fun pseudoLegalMoves(state: GameState): List<Move> {
        val moves = mutableListOf<Move>()
        val board = state.board
        val color = state.sideToMove
        for ((square, piece) in board.piecesOf(color)) {
            when (piece.type) {
                PieceType.PAWN -> generatePawnMoves(state, square, piece, moves)
                PieceType.KNIGHT -> generateStepMoves(board, square, piece, KNIGHT_OFFSETS, moves)
                PieceType.BISHOP -> generateSlidingMoves(board, square, piece, BISHOP_DIRS, moves)
                PieceType.ROOK -> generateSlidingMoves(board, square, piece, ROOK_DIRS, moves)
                PieceType.QUEEN -> generateSlidingMoves(board, square, piece, QUEEN_DIRS, moves)
                PieceType.KING -> {
                    generateStepMoves(board, square, piece, KING_OFFSETS, moves)
                    generateCastlingMoves(state, square, piece, moves)
                }
            }
        }
        return moves
    }

    /** Legal moves: pseudo-legal moves that don't leave the mover's own king in check. */
    fun legalMoves(state: GameState): List<Move> =
        pseudoLegalMoves(state).filter { move -> !isInCheck(applyMove(state, move), state.sideToMove) }

    fun legalMovesFrom(state: GameState, from: Square): List<Move> = legalMoves(state).filter { it.from == from }

    private fun generateStepMoves(
        board: Board, from: Square, piece: Piece, offsets: List<Pair<Int, Int>>, moves: MutableList<Move>
    ) {
        for ((df, dr) in offsets) {
            val to = Square.of(from.file + df, from.rank + dr) ?: continue
            val target = board.pieceAt(to)
            if (target == null) {
                moves.add(Move(from, to, piece))
            } else if (target.color != piece.color) {
                moves.add(Move(from, to, piece, capturedPiece = target, capturedSquare = to))
            }
        }
    }

    private fun generateSlidingMoves(
        board: Board, from: Square, piece: Piece, dirs: List<Pair<Int, Int>>, moves: MutableList<Move>
    ) {
        for ((df, dr) in dirs) {
            var f = from.file + df
            var r = from.rank + dr
            while (true) {
                val to = Square.of(f, r) ?: break
                val target = board.pieceAt(to)
                if (target == null) {
                    moves.add(Move(from, to, piece))
                } else {
                    if (target.color != piece.color) {
                        moves.add(Move(from, to, piece, capturedPiece = target, capturedSquare = to))
                    }
                    break
                }
                f += df; r += dr
            }
        }
    }

    private fun generatePawnMoves(state: GameState, from: Square, piece: Piece, moves: MutableList<Move>) {
        val board = state.board
        val dir = if (piece.color == Color.WHITE) 1 else -1
        val startRank = if (piece.color == Color.WHITE) 1 else 6
        val promotionRank = if (piece.color == Color.WHITE) 7 else 0

        val oneStep = Square.of(from.file, from.rank + dir)
        if (oneStep != null && board.pieceAt(oneStep) == null) {
            addPawnAdvance(from, oneStep, piece, promotionRank, moves)
            if (from.rank == startRank) {
                val twoStep = Square.of(from.file, from.rank + 2 * dir)
                if (twoStep != null && board.pieceAt(twoStep) == null) {
                    moves.add(Move(from, twoStep, piece, flag = MoveFlag.DOUBLE_PAWN_PUSH))
                }
            }
        }

        for (df in intArrayOf(-1, 1)) {
            val to = Square.of(from.file + df, from.rank + dir) ?: continue
            val target = board.pieceAt(to)
            if (target != null && target.color != piece.color) {
                addPawnCapture(from, to, piece, target, to, promotionRank, moves)
            } else if (target == null && state.enPassantTarget == to) {
                val capturedSquare = Square.of(to.file, from.rank)
                val capturedPiece = capturedSquare?.let { board.pieceAt(it) }
                if (capturedSquare != null && capturedPiece != null) {
                    moves.add(
                        Move(
                            from, to, piece,
                            capturedPiece = capturedPiece,
                            capturedSquare = capturedSquare,
                            flag = MoveFlag.EN_PASSANT
                        )
                    )
                }
            }
        }
    }

    private fun addPawnAdvance(from: Square, to: Square, piece: Piece, promotionRank: Int, moves: MutableList<Move>) {
        if (to.rank == promotionRank) {
            for (promo in listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)) {
                moves.add(Move(from, to, piece, promotion = promo, flag = MoveFlag.PROMOTION))
            }
        } else {
            moves.add(Move(from, to, piece))
        }
    }

    private fun addPawnCapture(
        from: Square, to: Square, piece: Piece, capturedPiece: Piece, capturedSquare: Square,
        promotionRank: Int, moves: MutableList<Move>
    ) {
        if (to.rank == promotionRank) {
            for (promo in listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)) {
                moves.add(
                    Move(from, to, piece, capturedPiece, capturedSquare, promotion = promo, flag = MoveFlag.PROMOTION)
                )
            }
        } else {
            moves.add(Move(from, to, piece, capturedPiece, capturedSquare))
        }
    }

    private fun generateCastlingMoves(state: GameState, kingSquare: Square, king: Piece, moves: MutableList<Move>) {
        val board = state.board
        val color = king.color
        val rank = if (color == Color.WHITE) 0 else 7
        if (kingSquare != Square(4, rank)) return
        val opponent = color.opposite()
        if (isSquareAttacked(board, kingSquare, opponent)) return

        val kingSideRight = if (color == Color.WHITE) state.castlingRights.whiteKingSide else state.castlingRights.blackKingSide
        val queenSideRight = if (color == Color.WHITE) state.castlingRights.whiteQueenSide else state.castlingRights.blackQueenSide

        if (kingSideRight) {
            val rookSquare = Square(7, rank)
            val rook = board.pieceAt(rookSquare)
            val fSquare = Square(5, rank)
            val gSquare = Square(6, rank)
            if (rook != null && rook.type == PieceType.ROOK && rook.color == color &&
                board.pieceAt(fSquare) == null && board.pieceAt(gSquare) == null &&
                !isSquareAttacked(board, fSquare, opponent) && !isSquareAttacked(board, gSquare, opponent)
            ) {
                moves.add(Move(kingSquare, gSquare, king, flag = MoveFlag.CASTLE_KINGSIDE))
            }
        }
        if (queenSideRight) {
            val rookSquare = Square(0, rank)
            val rook = board.pieceAt(rookSquare)
            val dSquare = Square(3, rank)
            val cSquare = Square(2, rank)
            val bSquare = Square(1, rank)
            if (rook != null && rook.type == PieceType.ROOK && rook.color == color &&
                board.pieceAt(dSquare) == null && board.pieceAt(cSquare) == null && board.pieceAt(bSquare) == null &&
                !isSquareAttacked(board, dSquare, opponent) && !isSquareAttacked(board, cSquare, opponent)
            ) {
                moves.add(Move(kingSquare, cSquare, king, flag = MoveFlag.CASTLE_QUEENSIDE))
            }
        }
    }

    /** Applies a (legal) move and returns the resulting new state. Does not itself validate legality. */
    fun applyMove(state: GameState, move: Move): GameState {
        val board = state.board.copy()
        val piece = move.piece

        move.capturedSquare?.let { board.setPiece(it, null) }
        board.setPiece(move.from, null)
        val placedPiece = if (move.promotion != null) Piece(piece.color, move.promotion) else piece
        board.setPiece(move.to, placedPiece)

        when (move.flag) {
            MoveFlag.CASTLE_KINGSIDE -> {
                val rank = move.from.rank
                val rook = board.pieceAt(Square(7, rank))
                board.setPiece(Square(7, rank), null)
                board.setPiece(Square(5, rank), rook)
            }
            MoveFlag.CASTLE_QUEENSIDE -> {
                val rank = move.from.rank
                val rook = board.pieceAt(Square(0, rank))
                board.setPiece(Square(0, rank), null)
                board.setPiece(Square(3, rank), rook)
            }
            else -> {}
        }

        var rights = state.castlingRights
        if (piece.type == PieceType.KING) {
            rights = if (piece.color == Color.WHITE) {
                rights.copy(whiteKingSide = false, whiteQueenSide = false)
            } else {
                rights.copy(blackKingSide = false, blackQueenSide = false)
            }
        }
        fun clearRightsIfRookSquare(square: Square) {
            rights = when (square) {
                Square(0, 0) -> rights.copy(whiteQueenSide = false)
                Square(7, 0) -> rights.copy(whiteKingSide = false)
                Square(0, 7) -> rights.copy(blackQueenSide = false)
                Square(7, 7) -> rights.copy(blackKingSide = false)
                else -> rights
            }
        }
        clearRightsIfRookSquare(move.from)
        move.capturedSquare?.let { clearRightsIfRookSquare(it) }

        val newEnPassant = if (move.flag == MoveFlag.DOUBLE_PAWN_PUSH) {
            Square(move.from.file, (move.from.rank + move.to.rank) / 2)
        } else null

        val newHalfMove = if (piece.type == PieceType.PAWN || move.isCapture) 0 else state.halfMoveClock + 1
        val newFullMove = if (state.sideToMove == Color.BLACK) state.fullMoveNumber + 1 else state.fullMoveNumber

        var newState = GameState(
            board = board,
            sideToMove = state.sideToMove.opposite(),
            castlingRights = rights,
            enPassantTarget = newEnPassant,
            halfMoveClock = newHalfMove,
            fullMoveNumber = newFullMove,
            moveHistory = state.moveHistory + move,
            positionCounts = state.positionCounts
        )
        val key = positionKey(newState)
        val counts = newState.positionCounts.toMutableMap()
        counts[key] = (counts[key] ?: 0) + 1
        newState = newState.copy(positionCounts = counts)
        return newState
    }

    /** Encodes piece placement, side to move, castling rights and en-passant file for repetition detection. */
    fun positionKey(state: GameState): String {
        val sb = StringBuilder(80)
        for (r in 7 downTo 0) {
            for (f in 0..7) {
                val p = state.board.pieceAt(Square(f, r))
                sb.append(if (p == null) '.' else pieceChar(p))
            }
        }
        sb.append(state.sideToMove)
        sb.append(state.castlingRights.whiteKingSide).append(state.castlingRights.whiteQueenSide)
        sb.append(state.castlingRights.blackKingSide).append(state.castlingRights.blackQueenSide)
        sb.append(state.enPassantTarget?.file ?: -1)
        return sb.toString()
    }

    private fun pieceChar(p: Piece): Char {
        val c = when (p.type) {
            PieceType.PAWN -> 'p'
            PieceType.KNIGHT -> 'n'
            PieceType.BISHOP -> 'b'
            PieceType.ROOK -> 'r'
            PieceType.QUEEN -> 'q'
            PieceType.KING -> 'k'
        }
        return if (p.color == Color.WHITE) c.uppercaseChar() else c
    }

    fun status(state: GameState): GameStatus {
        val inCheck = isInCheck(state, state.sideToMove)
        val hasLegalMoves = legalMoves(state).isNotEmpty()
        if (!hasLegalMoves) {
            return if (inCheck) GameStatus.CHECKMATE else GameStatus.STALEMATE
        }
        if (state.halfMoveClock >= 100) return GameStatus.DRAW_FIFTY_MOVE
        if ((state.positionCounts[positionKey(state)] ?: 0) >= 3) return GameStatus.DRAW_REPETITION
        if (hasInsufficientMaterial(state.board)) return GameStatus.DRAW_INSUFFICIENT_MATERIAL
        return if (inCheck) GameStatus.CHECK else GameStatus.ONGOING
    }

    private fun hasInsufficientMaterial(board: Board): Boolean {
        val allPieces = board.allPieces()
        val nonKings = allPieces.map { it.second }.filter { it.type != PieceType.KING }
        if (nonKings.isEmpty()) return true
        if (nonKings.size == 1 && (nonKings[0].type == PieceType.BISHOP || nonKings[0].type == PieceType.KNIGHT)) return true
        if (nonKings.size == 2 && nonKings.all { it.type == PieceType.BISHOP }) {
            val bishopSquares = allPieces.filter { it.second.type == PieceType.BISHOP }.map { it.first }
            val colors = nonKings.map { it.color }.toSet()
            if (colors.size == 2 && bishopSquares.size == 2) {
                val squareColor0 = (bishopSquares[0].file + bishopSquares[0].rank) % 2
                val squareColor1 = (bishopSquares[1].file + bishopSquares[1].rank) % 2
                if (squareColor0 == squareColor1) return true
            }
        }
        return false
    }
}
