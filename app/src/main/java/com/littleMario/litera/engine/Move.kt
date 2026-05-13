package com.littleMario.litera.engine

data class Move(
    val word: String,
    val row: Int,
    val col: Int,
    val direction: String,
    val score: Int
)