package com.example.literakowanie

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.DataInputStream
import java.io.IOException
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private var cachedDatabase: MutableList<String>? = null
    private lateinit var inputField: EditText
    private lateinit var wordList: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var infoLabel: TextView

    private val executorService = Executors.newFixedThreadPool(4)
    private val POLISH_LETTERS = "aąbcćdeęfghijklłmnńoópqrsśtuvwxyzźż"

    private val handler = android.os.Handler()
    private lateinit var searchRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        inputField = findViewById(R.id.inputField)
        wordList = findViewById(R.id.wordList)
        infoLabel = findViewById(R.id.infoLabel)
        val clearButton: Button = findViewById(R.id.clearButton)

        // Initialize database (load from cache or file)
        cachedDatabase = cachedDatabase ?: loadDatabaseFromCache() ?: loadDatabaseFromFile()

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        wordList.adapter = adapter

        // Initialize searchRunnable as an empty runnable
        searchRunnable = Runnable { }

        inputField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Cancel previous search if it's pending
                handler.removeCallbacks(searchRunnable)

                // Schedule a new search after a delay
                searchRunnable = Runnable {
                    searchWords(s.toString())
                }
                handler.postDelayed(searchRunnable, 300) // Adjust delay as needed
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        clearButton.setOnClickListener {
            inputField.text.clear()
            adapter.clear()
            infoLabel.text = ""
            infoLabel.setTextColor(getColor(android.R.color.holo_green_dark))
        }
    }

    private fun loadDatabaseFromFile(): MutableList<String> {
        val database = mutableListOf<String>()
        try {
            // Display loading message
            runOnUiThread {
                infoLabel.text = "Wczytuję bazę słów..."
                infoLabel.setTextColor(getColor(android.R.color.holo_red_dark))
            }

            // Load words from file
            val inputStream = resources.openRawResource(R.raw.words)
            val dataInputStream = DataInputStream(inputStream)

            while (dataInputStream.available() > 0) {
                val length = dataInputStream.readInt()  // read word length
                val bytes = ByteArray(length)
                dataInputStream.read(bytes)  // read bytes
                val word = String(bytes, Charsets.UTF_8)  // convert to String
                database.add(word)
            }

            // Close resources
            dataInputStream.close()

            // Clear loading message
            runOnUiThread {
                infoLabel.text = ""
                infoLabel.setTextColor(getColor(android.R.color.holo_green_dark))
            }

            // Save to cache
            cachedDatabase = database

        } catch (e: IOException) {
            e.printStackTrace()
            // Handle error if needed
        }
        return database
    }

    private fun loadDatabaseFromCache(): MutableList<String>? {
        // Return cached database if available
        return cachedDatabase
    }

    private fun searchWords(inputLetters: String) {
        val cleanedInputLetters = inputLetters.toLowerCase(Locale.getDefault()).replace("[^aąbcćdeęfghijklłmnńoópqrsśtuvwxyzźż\\s]".toRegex(), "")
        val letterCount = cleanedInputLetters.length

        runOnUiThread {
            infoLabel.text = "Szukam..."
            infoLabel.setTextColor(getColor(android.R.color.holo_red_dark))
        }

        executorService.submit {
            val foundWords = findWords(cachedDatabase ?: mutableListOf(), cleanedInputLetters, letterCount).sorted()
            runOnUiThread {
                adapter.clear()
                adapter.addAll(foundWords)
                if (foundWords.isEmpty()) {
                    infoLabel.text = "Nie znaleziono żadnego słowa."
                    infoLabel.setTextColor(getColor(android.R.color.holo_red_dark))
                } else {
                    infoLabel.text = "Oto pasujące słowa:"
                    infoLabel.setTextColor(getColor(android.R.color.holo_green_dark))
                }
            }
        }
    }

    private fun findWords(database: List<String>, inputLetters: String, letterCount: Int): List<String> {
        val foundWords = mutableListOf<String>()

        if (inputLetters.contains(" ")) {
            for (c in POLISH_LETTERS) {
                val inputWithReplacement = inputLetters.replaceFirst(" ", c.toString())
                foundWords.addAll(database.filter { canFormWord(it, inputWithReplacement, letterCount) })
            }
        } else {
            foundWords.addAll(database.filter { canFormWord(it, inputLetters, letterCount) })
        }

        return foundWords
    }

    private fun canFormWord(word: String, inputLetters: String, letterCount: Int): Boolean {
        if (word.length != letterCount) {
            return false
        }

        val letters = mutableMapOf<Char, Int>()
        for (letter in inputLetters) {
            letters[letter] = letters.getOrDefault(letter, 0) + 1
        }

        for (letter in word) {
            if (!letters.containsKey(letter) || letters[letter]!! < 1) {
                return false
            }
            letters[letter] = letters[letter]!! - 1
        }

        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        executorService.shutdown()
    }
}
