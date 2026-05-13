package com.littleMario.litera.engine

object CrossCheckBuilder {

    private val alphabet =
        "aąbcćdeęfghijklłmnńoópqrsśtuvwxyzźż"

    fun build(board: Board): Array<Array<Set<Char>>> {

        val size = 15

        return Array(size) {
            Array(size) {
                alphabet.toSet()
            }
        }
    }
}