package com.littleMario.litera.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.littleMario.litera.engine.Move

@Composable
fun MovePreview(moves: List<Move>) {

    Column {

        moves.take(10).forEach { move ->
            Text("${move.word}  ${move.score}")
        }
    }
}