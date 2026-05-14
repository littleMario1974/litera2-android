package com.littleMario.litera.engine

class Board {

    companion object {
        const val SIZE = 15
    }

    val grid =
        Array(SIZE) {
            arrayOfNulls<Char>(SIZE)
        }

    fun get(x: Int, y: Int): Char? {

        return if (
            x in 0 until SIZE &&
            y in 0 until SIZE
        ) {
            grid[x][y]
        } else {
            null
        }
    }

    fun set(
        x: Int,
        y: Int,
        c: Char?
    ) {

        if (
            x in 0 until SIZE &&
            y in 0 until SIZE
        ) {
            grid[x][y] = c
        }
    }

    fun clear() {

        for (x in 0 until SIZE) {
            for (y in 0 until SIZE) {
                grid[x][y] = null
            }
        }
    }

    fun isEmpty(): Boolean {

        return grid.all { row ->
            row.all { it == null }
        }
    }

    fun inBounds(
        x: Int,
        y: Int
    ): Boolean {

        return x in 0 until SIZE &&
                y in 0 until SIZE
    }

    fun hasAnyTile(): Boolean {

        return grid.any { row ->
            row.any { it != null }
        }
    }
}