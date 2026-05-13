package com.littleMario.litera.engine

import java.io.*
import com.littleMario.litera.engine.Node
class DawgBuilder {

    companion object {
        const val ALPHABET = "aąbcćdeęfghijklłmnńoópqrsśtuvwxyzźż"
        const val SIZE = ALPHABET.length

        private val CHAR_TO_INDEX = IntArray(65536) { -1 }

        init {
            for (i in ALPHABET.indices) {
                CHAR_TO_INDEX[ALPHABET[i].code] = i
            }
        }
    }


    private data class Unchecked(
        val parent: Node,
        val char: Int,
        val child: Node
    )

    private val root = Node()
    private val unchecked = ArrayDeque<Unchecked>()
    private val minimized = HashMap<Int, Node>()

    private var previousWord = ""

    fun build(words: List<String>): Node {

        val sorted = words.sorted()

        for (w in sorted) {
            insert(w)
        }

        minimize(0)
        return root
    }

    private fun insert(word: String) {

        val prefix = commonPrefix(word, previousWord)

        minimize(prefix)

        var node = root

        for (i in 0 until prefix) {
            node = node.next[idx(word[i])]!!
        }

        for (i in prefix until word.length) {

            val c = idx(word[i])
            val next = Node()

            node.next[c] = next
            unchecked.addLast(Unchecked(node, c, next))

            node = next
        }

        node.terminal = true
        previousWord = word
    }

    private fun minimize(downTo: Int) {

        while (unchecked.size > downTo) {

            val u = unchecked.removeLast()
            val key = fingerprint(u.child)

            val existing = minimized[key]

            if (existing != null) {
                u.parent.next[u.char] = existing
            } else {
                minimized[key] = u.child
            }
        }
    }

    private fun fingerprint(node: Node): Int {

        var h = if (node.terminal) 1 else 0

        for (i in 0 until SIZE) {
            val child = node.next[i] ?: continue
            h = h * 31 + i + System.identityHashCode(child)
        }

        return h
    }

    private fun idx(c: Char): Int {
        val i = CHAR_TO_INDEX[c.code]
        require(i >= 0) { "Unknown char: $c" }
        return i
    }

    private fun commonPrefix(a: String, b: String): Int {
        val len = minOf(a.length, b.length)
        for (i in 0 until len) {
            if (a[i] != b[i]) return i
        }
        return len
    }

    // -----------------------------------------------------
    // ROOT ACCESS (ważne dla debug)
    // -----------------------------------------------------

    fun getRoot(): Node = root
}