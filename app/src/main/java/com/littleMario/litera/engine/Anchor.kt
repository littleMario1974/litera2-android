package com.littleMario.litera.engine

class Anchor {

    fun get(board: Board): List<Pair<Int, Int>> {

        val res = mutableListOf<Pair<Int, Int>>()

        for (y in 0 until Board.SIZE)
            for (x in 0 until Board.SIZE)
                if (board.get(x,y)==null &&
                    (board.isEmpty() || hasNeighbor(board,x,y)))
                    res.add(x to y)

        return res
    }

    private fun hasNeighbor(b: Board, x:Int,y:Int):Boolean =
        listOf(1 to 0,-1 to 0,0 to 1,0 to -1)
            .any { (dx,dy)-> b.get(x+dx,y+dy)!=null }
}