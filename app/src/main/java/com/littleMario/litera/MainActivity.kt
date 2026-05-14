package com.littleMario.litera

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
import com.littleMario.litera.engine.*
import com.littleMario.litera.ui.LiterakiScreen
import java.io.DataInputStream

class MainActivity : AppCompatActivity() {

    private lateinit var root: Node
    private var engineBoard: Board = Board()

    private val boardState =
        mutableStateOf(Array(15) { Array<Char?>(15) { null } })

    val rack = mutableStateOf(
        mutableListOf('k', 'o', 't', 'a', 'm', 'r', 'y')
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val reader = DawgReader()

        root = reader.load(
            DataInputStream(assets.open("dictionary.dawg"))
        )

        engineBoard = Board()

        setContent {
            LiterakiScreen(
                boardState = boardState,
                rack = rack,
                solver = { boardArray, letters ->

                    // RESET PLANSZY
                    for (r in 0 until 15) {
                        for (c in 0 until 15) {
                            engineBoard.set(r, c, null)
                        }
                    }

                    // KOPIA STANU UI -> ENGINE
                    for (r in 0 until 15) {
                        for (c in 0 until 15) {
                            boardArray[r][c]?.let {
                                engineBoard.set(r, c, it)
                            }
                        }
                    }

                    val rackMap = letters.groupingBy { it }.eachCount()

                    // ENGINE (ZAWSZE NON-NULL ROOT)
                    val engine = MoveFinderV10(
                        root = root,
                        board = engineBoard,
                        anchor = Anchor(),
                        validator = MoveValidator(root),
                        scoring = Scoring(BonusTiles()),
                        crossCheck = CrossCheckBuilder.build(engineBoard)
                    )

                    val moves = engine.find(rackMap)

                    println("MOVES = ${moves.size}")

                    moves
                }
            )
        }
    }
}