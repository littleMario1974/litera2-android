package com.littleMario.litera.engine

class Board {

    companion object { const val SIZE = 15 }

    val grid = Array(SIZE) { arrayOfNulls<Char>(SIZE) }

    fun get(x: Int, y: Int): Char? =
        if (x in 0 until SIZE && y in 0 until SIZE) grid[y][x] else null

    fun set(x: Int, y: Int, c: Char) {
        if (x in 0 until SIZE && y in 0 until SIZE) {
            grid[y][x] = c
        }
    }

    fun isEmpty(): Boolean =
        grid.all { row -> row.all { it == null } }

    fun inBounds(r: Int, c: Int): Boolean {
        return r in 0 until 15 && c in 0 until 15
    }
}