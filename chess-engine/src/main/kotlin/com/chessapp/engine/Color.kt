package com.chessapp.engine

enum class Color {
    WHITE, BLACK;

    fun opposite(): Color = if (this == WHITE) BLACK else WHITE
}
