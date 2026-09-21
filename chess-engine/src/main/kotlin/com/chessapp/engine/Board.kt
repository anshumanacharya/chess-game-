package com.chessapp.engine

/**
 * Mutable 8x8 board, indexed [file][rank]. Instances are always accessed through
 * [copy] before mutation by the move generator so [GameState] snapshots stay immutable.
 */
class Board private constructor(private val squares: Array<Array<Piece?>>) {

    fun pieceAt(square: Square): Piece? = squares[square.file][square.rank]

    fun setPiece(square: Square, piece: Piece?) {
        squares[square.file][square.rank] = piece
    }

    fun copy(): Board {
        val newSquares = Array(8) { f -> Array<Piece?>(8) { r -> squares[f][r] } }
        return Board(newSquares)
    }

    fun findKing(color: Color): Square {
        for (f in 0..7) for (r in 0..7) {
            val p = squares[f][r]
            if (p != null && p.color == color && p.type == PieceType.KING) return Square(f, r)
        }
        error("King not found for $color")
    }

    fun allPieces(): List<Pair<Square, Piece>> {
        val list = mutableListOf<Pair<Square, Piece>>()
        for (f in 0..7) for (r in 0..7) {
            squares[f][r]?.let { list.add(Square(f, r) to it) }
        }
        return list
    }

    fun piecesOf(color: Color): List<Pair<Square, Piece>> = allPieces().filter { it.second.color == color }

    companion object {
        fun initialPosition(): Board {
            val squares = Array<Array<Piece?>>(8) { arrayOfNulls(8) }
            val backRank = listOf(
                PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN,
                PieceType.KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK
            )
            for (file in 0..7) {
                squares[file][0] = Piece(Color.WHITE, backRank[file])
                squares[file][1] = Piece(Color.WHITE, PieceType.PAWN)
                squares[file][6] = Piece(Color.BLACK, PieceType.PAWN)
                squares[file][7] = Piece(Color.BLACK, backRank[file])
            }
            return Board(squares)
        }

        fun empty(): Board = Board(Array(8) { arrayOfNulls(8) })
    }
}
