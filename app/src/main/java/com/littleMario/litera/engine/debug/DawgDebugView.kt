package com.littleMario.litera.engine.debug

import com.littleMario.litera.engine.Node

class DawgDebugView(
    private val root: Node,
    private val alphabet: String
) {

    private val size = alphabet.length

    // -----------------------------------------------------

    fun printTree(maxDepth: Int = 4) {
        println("=== DAWG TREE ===")
        dfs(root, "", 0, maxDepth, HashSet())
    }

    // -----------------------------------------------------

    fun printWordPath(word: String) {

        println("=== WORD PATH: $word ===")

        var node = root

        for (i in word.indices) {

            val idx = alphabet.indexOf(word[i])
            if (idx < 0) {
                println("❌ UNKNOWN CHAR '${word[i]}'")
                return
            }

            val next = node.next[idx]

            if (next == null) {
                println("❌ BROKEN at '${word[i]}' (pos $i)")
                return
            }

            println("✔ '${word[i]}' → terminal=${next.terminal}")

            node = next
        }

        println("✔ END terminal=${node.terminal}")
    }

    // -----------------------------------------------------

    private fun dfs(
        node: Node,
        prefix: String,
        depth: Int,
        maxDepth: Int,
        visited: HashSet<Node>
    ) {

        if (depth > maxDepth) return
        if (!visited.add(node)) return

        val mark = if (node.terminal) "*" else ""

        println("${" ".repeat(depth * 2)}$prefix$mark")

        for (i in 0 until size) {

            val child = node.next[i] ?: continue

            dfs(child, prefix + alphabet[i], depth + 1, maxDepth, visited)
        }
    }
}