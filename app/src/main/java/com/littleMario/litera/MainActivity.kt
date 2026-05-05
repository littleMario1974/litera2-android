package com.littleMario.litera

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.*
import java.io.DataInputStream
import java.io.IOException
import java.text.Collator
import java.util.*
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var database: List<String>
    private lateinit var wordFreqCache: Map<String, Map<Char, Int>>
    private lateinit var wordsByLength: Map<Int, List<String>>

    private lateinit var inputField: EditText
    private lateinit var wordList: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var infoLabel: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var searchAllButton: Button

    private val executorService = Executors.newFixedThreadPool(4)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        MobileAds.initialize(this) {}

        inputField = findViewById(R.id.inputField)
        wordList = findViewById(R.id.wordList)
        infoLabel = findViewById(R.id.infoLabel)
        progressBar = findViewById(R.id.progressBar)
        searchAllButton = findViewById(R.id.searchAllButton)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        wordList.adapter = adapter

        progressBar.visibility = View.GONE

        loadDatabaseFromFile()

        searchAllButton.setOnClickListener {
            searchAllWords(inputField.text.toString())
        }
    }

    // =========================
    // ULTRA FAST LOAD + CACHE
    // =========================
    private fun loadDatabaseFromFile() {
        progressBar.visibility = View.VISIBLE

        executorService.submit {
            val list = mutableListOf<String>()

            try {
                val inputStream = resources.openRawResource(R.raw.words)
                val dataInputStream = DataInputStream(inputStream)

                while (dataInputStream.available() > 0) {
                    val length = dataInputStream.readInt()
                    val bytes = ByteArray(length)
                    dataInputStream.read(bytes)
                    list.add(String(bytes, Charsets.UTF_8))
                }

                dataInputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            database = list

            // CACHE 1: frequency map
            wordFreqCache = database.associateWith { word ->
                word.groupingBy { it }.eachCount()
            }

            // CACHE 2: by length
            wordsByLength = database.groupBy { it.length }

            runOnUiThread {
                progressBar.visibility = View.GONE
                inputField.visibility = View.VISIBLE
                searchAllButton.visibility = View.VISIBLE
            }
        }
    }

    // =========================
    // NORMALIZACJA (? = spacja)
    // =========================
    private fun normalize(input: String): String {
        return input
            .lowercase(Locale.getDefault())
            .replace('?', ' ')
    }

    // =========================
    // ULTRA FAST SEARCH ALL
    // =========================
    private fun searchAllWords(input: String) {

        val cleaned = normalize(input)

        if (cleaned.length < 2) {
            Toast.makeText(this, "Wpisz min. 2 znaki", Toast.LENGTH_SHORT).show()
            return
        }

        val inputCounter = cleaned.groupingBy { it }.eachCount()

        runOnUiThread {
            infoLabel.text = "Szukam..."
            infoLabel.visibility = View.VISIBLE
        }

        executorService.submit {

            val candidates = wordsByLength[cleaned.length] ?: emptyList()

            val result = ArrayList<String>(5000)

            for (word in candidates) {
                val wc = wordFreqCache[word] ?: continue
                if (canBuild(word, wc, inputCounter)) {
                    result.add(word)
                }
            }

            val sorted = result.distinct().sortedWith(
                compareByDescending<String> { it.length }
                    .thenComparing(Collator.getInstance(Locale("pl", "PL")))
            )

            runOnUiThread {
                adapter.clear()
                adapter.addAll(sorted)

                infoLabel.text = if (sorted.isEmpty())
                    "Brak wyników"
                else
                    "Znaleziono: ${sorted.size}"
            }
        }
    }

    // =========================
    // FAST CHECK (NO ALLOCATION)
    // =========================
    private fun canBuild(
        word: String,
        wordMap: Map<Char, Int>,
        inputMap: Map<Char, Int>
    ): Boolean {

        for ((c, count) in wordMap) {
            if (inputMap.getOrDefault(c, 0) < count) return false
        }

        return true
    }

    override fun onDestroy() {
        executorService.shutdown()
        super.onDestroy()
    }
}
