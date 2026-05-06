package com.littleMario.litera.engine

data class Move(
    val word: String,
    val x: Int,
    val y: Int,
    val horizontal: Boolean,
    val score: Int,
    val crossWords: List<CrossWord>
)