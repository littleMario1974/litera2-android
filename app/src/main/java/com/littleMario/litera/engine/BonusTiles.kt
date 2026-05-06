package com.littleMario.litera.engine

enum class Bonus { NONE, DL, TL, DW, TW }

class BonusTiles {

    private val grid = Array(15) { Array(15) { Bonus.NONE } }

    fun get(x: Int, y: Int): Bonus = grid[y][x]
}