package com.littleMario.litera.engine

class MoveValidator(private val root: Node) {

    private val alphabet = "aąbcćdeęfghijklłmnńoópqrsśtuvwxyzźż"

    fun isValid(word: String, crossWords: List<String>): Boolean {
        if (!exists(word)) return false
        return crossWords.all { exists(it) }
    }

    private fun exists(word: String): Boolean {

        var node = root

        for (c in word) {
            val i = alphabet.indexOf(c)
            if (i == -1) return false
            node = node.next[i] ?: return false
        }

        return node.terminal
    }
}