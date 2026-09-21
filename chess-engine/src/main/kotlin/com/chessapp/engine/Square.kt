package com.chessapp.engine

/**
 * A board square. `file` is 0-7 (a-h), `rank` is 0-7 (1-8).
 */
data class Square(val file: Int, val rank: Int) {
    init {
        require(file in 0..7 && rank in 0..7) { "Square out of bounds: file=$file, rank=$rank" }
    }

    val algebraic: String
        get() = "${'a' + file}${rank + 1}"

    override fun toString(): String = algebraic

    companion object {
        /** Returns null instead of throwing when the coordinates are off-board. */
        fun of(file: Int, rank: Int): Square? =
            if (file in 0..7 && rank in 0..7) Square(file, rank) else null

        fun fromAlgebraic(s: String): Square {
            require(s.length == 2) { "Invalid algebraic square: $s" }
            val file = s[0].lowercaseChar() - 'a'
            val rank = s[1] - '1'
            return Square(file, rank)
        }
    }
}
