package com.littleMario.litera.engine

enum class Bonus { NONE, DL, TL, DW, TW }

class BonusTiles {

    private val grid = Array(15) { Array(15) { Bonus.NONE } }

    fun get(x: Int, y: Int): Bonus {

        if (x !in 0 until 15 || y !in 0 until 15) {
            return Bonus.NONE
        }

        return grid[y][x]
    }
}