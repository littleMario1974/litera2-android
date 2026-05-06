package com.littleMario.litera

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.littleMario.litera.engine.*
import java.io.DataInputStream

class MainActivity : AppCompatActivity() {

    private lateinit var board: Board
    private lateinit var engine: MoveFinderV6

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        board = Board()

        val reader = DawgReader()
        reader.load(DataInputStream(resources.openRawResource(R.raw.dictionary_dawg)))

        val validator = MoveValidator(reader.getRoot())
        val scoring = Scoring(BonusTiles())

        engine = MoveFinderV6(
            reader.getRoot(),
            board,
            Anchor(),
            validator,
            scoring
        )

        val rack = mapOf('k' to 1, 'o' to 1, 't' to 1, 'a' to 1)

        val moves = engine.find(rack)

        moves.take(10).forEach {
            println(it)
        }
    }
}