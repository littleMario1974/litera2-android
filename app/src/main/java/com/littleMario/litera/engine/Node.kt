package com.littleMario.litera.engine

class Node(
    val next: Array<Node?> = arrayOfNulls(35),
    var terminal: Boolean = false
)