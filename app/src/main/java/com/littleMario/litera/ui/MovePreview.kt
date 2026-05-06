package com.littleMario.litera.ui

import com.littleMario.litera.engine.Move

class MovePreview {

    fun show(move: Move) {
        println("BEST: ${move.word} -> ${move.score}")
    }
}