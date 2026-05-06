package com.littleMario.litera.engine

import java.io.DataInputStream

class DawgReader {

    private val alphabet = "aąbcćdeęfghijklłmnńoópqrsśtuvwxyzźż"

    private lateinit var root: Node

    fun load(input: DataInputStream) {
        root = read(input)
    }

    private fun read(input: DataInputStream): Node {

        val node = Node()
        node.terminal = input.readBoolean()

        for (i in alphabet.indices) {
            if (input.readBoolean()) {
                node.next[i] = read(input)
            }
        }

        return node
    }

    fun getRoot() = root
}