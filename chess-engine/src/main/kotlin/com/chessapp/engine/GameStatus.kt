package com.chessapp.engine

enum class GameStatus {
    ONGOING,
    CHECK,
    CHECKMATE,
    STALEMATE,
    DRAW_FIFTY_MOVE,
    DRAW_INSUFFICIENT_MATERIAL,
    DRAW_REPETITION;

    val isGameOver: Boolean
        get() = this != ONGOING && this != CHECK
}
