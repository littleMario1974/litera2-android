package com.littleMario.litera

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
import com.littleMario.litera.engine.*
import com.littleMario.litera.engine.debug.DawgDebugView
import com.littleMario.litera.engine.debug.DawgValidator
import com.littleMario.litera.ui.LiterakiScreen
import java.io.DataInputStream

class MainActivity : AppCompatActivity() {

    private lateinit var engine: MoveFinderV9

    private val alphabet =
        "aąbcćdeęfghijklłmnńoópqrsśtuvwxyzźż"

    private val boardState =
        mutableStateOf(Array(15) { Array<Char?>(15) { null } })

    val rack = mutableStateOf(
        mutableListOf('k', 'o', 't', 'a', 'm', 'r', 'y')
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val reader = DawgReader()

        val root = reader.load(
            DataInputStream(assets.open("dictionary.dawg"))
        )

        println("✅ DAWG LOADED")

        val board = Board()

        // 🔥 V9 ENGINE (GLOBALNY)
        engine = MoveFinderV9(
            root,
            board,
            Anchor(),
            MoveValidator(root),
            Scoring(BonusTiles()),
            CrossCheckBuilder.build(board)
        )

        println("✅ ENGINE READY")

        setContent {
            LiterakiScreen(
                boardState = boardState,
                rack = rack,
                solver = { boardArray, letters ->

                    println("🔥 SOLVER START")

                    val engineBoard = boardArray.toBoard()

                    val rackMap = letters.groupingBy { it }.eachCount()

                    // 🔥 KLUCZ: V9 używa aktualnej planszy
                    val moves = engine.find(rackMap)

                    println("🔥 MOVES FOUND: ${moves.size}")

                    moves
                }
            )
        }
    }
}

fun Array<Array<Char?>>.toBoard(): Board {

    val board = Board()

    for (r in 0 until 15) {
        for (c in 0 until 15) {
            val ch = this[r][c]
            if (ch != null) {
                board.set(r, c, ch)
            }
        }
    }

    return board
}