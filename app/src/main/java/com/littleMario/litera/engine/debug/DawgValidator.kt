package com.littleMario.litera.engine.debug

import com.littleMario.litera.engine.Node

class DawgValidator(
    private val root: Node,
    private val alphabet: String
) {

    private val index = IntArray(65536) { -1 }

    init {
        for (i in alphabet.indices) {
            index[alphabet[i].code] = i
        }
    }

    fun checkAll(words: List<String>) {

        var missing = 0
        var found = 0

        for (w in words) {

            if (contains(w)) {
                found++
            } else {
                missing++
                println("❌ MISSING: $w")
            }
        }

        println("=== DAWG VALIDATION ===")
        println("FOUND: $found")
        println("MISSING: $missing")

        if (missing == 0) {
            println("✅ DAWG IS COMPLETE")
        } else {
            println("⚠️ DAWG IS INCOMPLETE")
        }
    }

    private fun contains(word: String): Boolean {

        var node = root

        for (c in word) {

            val i = index[c.code]
            if (i < 0) return false

            val next = node.next[i] ?: return false
            node = next
        }

        return node.terminal
    }
}