package com.littleMario.litera.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.littleMario.litera.ui.getBonusColor

@Composable
fun BoardView(
    board: Array<Array<Char?>>,
    onCellClick: (Int, Int) -> Unit
) {

    Column(
        modifier = Modifier
            .background(Color(0xFF1B5E20))
            .padding(4.dp)
    ) {

        for (r in 0 until 15) {

            Row {

                for (c in 0 until 15) {

                    val bonus = getBonusColor(r, c)
                    val letter = board[r][c]

                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(bonus)
                            .border(1.dp, Color.Black)
                            .clickable { onCellClick(r, c) }
                    ) {
                        Text(
                            text = letter?.toString() ?: "",
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}