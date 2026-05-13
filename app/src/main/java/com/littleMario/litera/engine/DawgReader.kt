package com.littleMario.litera.engine

import java.io.DataInputStream

class DawgReader {

    fun load(input: DataInputStream): Node {

        val size = input.readInt()

        val nodes = Array(size) { Node() }

        // 1. najpierw tworzymy wszystkie węzły
        for (i in 0 until size) {
            nodes[i] = Node()
        }

        // 2. teraz uzupełniamy dane
        for (i in 0 until size) {

            val terminal = input.readBoolean()
            val edgeCount = input.readInt()

            val node = nodes[i]
            node.terminal = terminal

            repeat(edgeCount) {

                val charIdx = input.readInt()
                val targetId = input.readInt()

                node.next[charIdx] = nodes[targetId]
            }
        }

        return nodes[0]
    }
}