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

    private lateinit var engine: MoveFinderV8

    // -------------------------------------------------
    // GLOBAL ALPHABET
    // -------------------------------------------------
    private val alphabet =
        "aąbcćdeęfghijklłmnńoópqrsśtuvwxyzźż"

    // -------------------------------------------------
    // BOARD STATE (UI)
    // -------------------------------------------------
    private val boardState =
        mutableStateOf(
            Array(15) { Array<Char?>(15) { null } }
        )

    // -------------------------------------------------
    // RACK
    // -------------------------------------------------
    val rack = mutableStateOf(
        mutableListOf('k', 'o', 't', 'a', 'm', 'r', 'y')
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // -------------------------------------------------
        // LOAD DAWG
        // -------------------------------------------------
        val reader = DawgReader()

        val root = reader.load(
            DataInputStream(assets.open("dictionary.dawg"))
        )

        println("✅ DAWG LOADED")

        // -------------------------------------------------
        // DEBUG
        // -------------------------------------------------
        val debug = DawgDebugView(root, alphabet)

        debug.printTree(maxDepth = 3)
        debug.printWordPath("kot")
        debug.printWordPath("dom")
        debug.printWordPath("żaba")

        val testWords = listOf(
            "kot",
            "dom",
            "żaba",
            "pies",
            "samochód",
            "książka"
        )

        DawgValidator(root, alphabet).checkAll(testWords)

        println("ALPHABET CHECK:")
        listOf('k', 'o', 't', 'a', 'm', 'r', 'y').forEach {
            println("$it -> ${alphabet.indexOf(it)}")
        }

        // -------------------------------------------------
        // ENGINE (NA PUSTEJ PLANSZY – START)
        // -------------------------------------------------
        val board = Board()

        engine = MoveFinderV8(
            root,
            board,
            Anchor(),
            MoveValidator(root),
            Scoring(BonusTiles()),
            CrossCheckBuilder.build(board)
        )

        println("✅ ENGINE READY")

        // -------------------------------------------------
        // UI
        // -------------------------------------------------
        setContent {
            LiterakiScreen(
                boardState = boardState,
                rack = rack,
                solver = { boardArray, letters ->

                    println("🔥 SOLVER START")
                    println("letters = $letters")

                    // KONWERSJA UI -> ENGINE BOARD
                    val engineBoard = boardArray.toBoard()

                    val anchors = Anchor().get(engineBoard)
                    println("ANCHORS = $anchors")

                    val rackMap = letters.groupingBy { it }.eachCount()

                    val moves = engine.find(rackMap)

                    println("🔥 MOVES FOUND: ${moves.size}")

                    moves
                }
            )
        }
    }
}

// -------------------------------------------------
// KONWERTER UI -> ENGINE BOARD
// -------------------------------------------------
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

