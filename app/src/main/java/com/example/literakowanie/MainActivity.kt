package com.example.literakowanie

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.DataInputStream
import java.io.IOException
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var database: MutableList<String>
    private lateinit var inputField: EditText
    private lateinit var wordList: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var infoLabel: TextView
    private lateinit var progressBar: ProgressBar

    private val executorService = Executors.newFixedThreadPool(4)
    private val POLISH_LETTERS = "aąbcćdeęfghijklłmnńoópqrsśtuvwxyzźż"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        inputField = findViewById(R.id.inputField)
        wordList = findViewById(R.id.wordList)
        infoLabel = findViewById(R.id.infoLabel)
        progressBar = findViewById(R.id.progressBar)
        val clearButton: Button = findViewById(R.id.clearButton)
        val searchAllButton: Button = findViewById(R.id.searchAllButton)
        val closeButton: Button = findViewById(R.id.closeButton)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        wordList.adapter = adapter

        loadDatabaseFromFile()

        inputField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchWords(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        clearButton.setOnClickListener {
            inputField.text.clear()
            adapter.clear()
            infoLabel.text = ""
            infoLabel.setTextColor(getColor(android.R.color.holo_green_dark))
        }

        searchAllButton.setOnClickListener {
            val inputText = inputField.text.toString().trim()
            if (inputText.length >= 3) {
                searchAllWords(inputText)
            } else {
                Toast.makeText(this, "Wprowadź przynajmniej trzy litery do wyszukania wszystkich słów.", Toast.LENGTH_SHORT).show()
            }
        }

        closeButton.setOnClickListener {
            finish()
        }
    }

    private fun loadDatabaseFromFile() {
        progressBar.visibility = View.VISIBLE
        executorService.submit {
            val database = mutableListOf<String>()
            try {
                val inputStream = resources.openRawResource(R.raw.words)
                val dataInputStream = DataInputStream(inputStream)

                while (dataInputStream.available() > 0) {
                    val length = dataInputStream.readInt()  // read word length
                    val bytes = ByteArray(length)
                    dataInputStream.read(bytes)  // read bytes
                    val word = String(bytes, Charsets.UTF_8)  // convert to String
                    database.add(word)
                }

                dataInputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            this.database = database

            runOnUiThread {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun searchWords(inputLetters: String) {
        val cleanedInputLetters = inputLetters.lowercase(Locale.getDefault()).replace("[^aąbcćdeęfghijklłmnńoópqrsśtuvwxyzźż\\s]".toRegex(), "")
        val letterCount = cleanedInputLetters.length

        runOnUiThread {
            infoLabel.text = "Szukam..."
            infoLabel.setTextColor(getColor(android.R.color.holo_red_dark))
        }

        executorService.submit {
            val foundWords = findWords(database, cleanedInputLetters, letterCount).sorted()
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

    private fun searchAllWords(inputLetters: String) {
        val cleanedInputLetters = inputLetters.lowercase(Locale.getDefault()).replace("[^aąbcćdeęfghijklłmnńoópqrsśtuvwxyzźż\\s]".toRegex(), "")

        runOnUiThread {
            infoLabel.text = "Szukam wszystkich słów..."
            infoLabel.setTextColor(getColor(android.R.color.holo_red_dark))
        }

        executorService.submit {
            val foundWords = findAllWords(database, cleanedInputLetters).sortedWith(compareByDescending<String> { it.length }.thenBy { it })
            runOnUiThread {
                adapter.clear()
                adapter.addAll(foundWords)
                if (foundWords.isEmpty()) {
                    infoLabel.text = "Nie znaleziono żadnego słowa."
                    infoLabel.setTextColor(getColor(android.R.color.holo_red_dark))
                } else {
                    infoLabel.text = "Oto wszystkie możliwe słowa:"
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

    private fun findAllWords(database: List<String>, inputLetters: String): List<String> {
        val foundWords = mutableListOf<String>()
        for (word in database) {
            if (canFormWord(word, inputLetters, word.length)) {
                foundWords.add(word)
            }
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

