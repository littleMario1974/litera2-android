package com.example.literakowanie

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
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
    private lateinit var clearButton: Button
    private lateinit var searchAllButton: Button

    private val executorService = Executors.newFixedThreadPool(4)
    private val POLISH_LETTERS = "aąbcćdeęfghijklłmnńoópqrsśtuvwxyzźż"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        inputField = findViewById(R.id.inputField)
        wordList = findViewById(R.id.wordList)
        infoLabel = findViewById(R.id.infoLabel)
        progressBar = findViewById(R.id.progressBar)
        clearButton = findViewById(R.id.clearButton)
        searchAllButton = findViewById(R.id.searchAllButton)
        val closeButton: ImageButton = findViewById(R.id.closeButton)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        wordList.adapter = adapter

        // Ukrycie infoLabel, progressBar, przycisków "Wyczyść" i "Wyszukaj wszystkie" oraz inputField na początku
        infoLabel.visibility = View.INVISIBLE
        progressBar.visibility = View.GONE
        clearButton.visibility = View.GONE
        searchAllButton.visibility = View.GONE
        inputField.visibility = View.GONE

        loadDatabaseFromFile()

        inputField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s != null) {
                    val newText = s.toString()

                    // Wyczyszczenie adaptera przechowującego znalezione słowa
                    adapter.clear()

                    // Ukrycie infoLabel po zmianie tekstu
                    infoLabel.visibility = View.INVISIBLE

                    // Walidacja ilości spacji
                    val numSpaces = newText.count { it == ' ' }
                    if (numSpaces > 1) {
                        Toast.makeText(
                            this@MainActivity,
                            "Dozwolona jedna spacja.",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Usunięcie ostatniej spacji
                        val sanitizedText = newText.replace(" ", "", true)
                        inputField.setText(sanitizedText)
                        inputField.setSelection(inputField.length()) // Ustawienie kursora na końcu
                        return
                    }

                    // Walidacja niedozwolonych znaków
                    val disallowedChar = newText.find { it !in POLISH_LETTERS && it != ' ' }
                    if (disallowedChar != null) {
                        Toast.makeText(
                            this@MainActivity,
                            "Niedozwolony znak.",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Usunięcie niedozwolonego znaku
                        val sanitizedText = newText.replace(disallowedChar.toString(), "", true)
                        inputField.setText(sanitizedText)
                        inputField.setSelection(inputField.length()) // Ustawienie kursora na końcu
                        return
                    }

                    // Jeśli nowy tekst jest niepusty, rozpocznij wyszukiwanie
                    if (newText.isNotBlank()) {
                        searchWords(newText)
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })


        clearButton.setOnClickListener {
            inputField.text.clear()
            adapter.clear()
            // Ukrycie infoLabel po wciśnięciu "Wyczyść"
            infoLabel.visibility = View.INVISIBLE
        }

        searchAllButton.setOnClickListener {
            val inputText = inputField.text.toString().trim()
            if (inputText.length >= 3) {
                searchAllWords(inputText)
            } else {
                Toast.makeText(
                    this,
                    "Wprowadź przynajmniej trzy litery do wyszukania wszystkich słów.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        closeButton.setOnClickListener {
            finish()
        }
    }

    private fun loadDatabaseFromFile() {
        progressBar.visibility = View.VISIBLE
        progressBar.progress = 0 // Ustawienie początkowego postępu

        executorService.submit {
            val database = mutableListOf<String>()
            try {
                val inputStream = resources.openRawResource(R.raw.words)
                val dataInputStream = DataInputStream(inputStream)

                val fileSize = inputStream.available().toFloat()
                var totalRead = 0f
                var bytesRead = 0

                while (dataInputStream.available() > 0) {
                    val length = dataInputStream.readInt()  // read word length
                    val bytes = ByteArray(length)
                    dataInputStream.read(bytes)  // read bytes
                    val word = String(bytes, Charsets.UTF_8)  // convert to String
                    database.add(word)

                    totalRead += length
                    bytesRead += length

                    // Aktualizacja ProgressBar co 10% postępu
                    if (bytesRead >= fileSize * 0.1 || dataInputStream.available() == 0) {
                        val progress = ((totalRead / fileSize) * 100).toInt()
                        runOnUiThread {
                            progressBar.progress = progress
                        }
                        bytesRead = 0
                    }
                }

                dataInputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            this.database = database

            runOnUiThread {
                progressBar.visibility = View.GONE
                inputField.visibility = View.VISIBLE // Pokaż pole do wpisywania liter po wczytaniu bazy
                clearButton.visibility = View.VISIBLE // Pokaż przycisk "Wyczyść" po wczytaniu bazy
                searchAllButton.visibility = View.VISIBLE // Pokaż przycisk "Wyszukaj wszystkie" po wczytaniu bazy

                // Ustawienie fokusu na pole do wpisywania liter i pokazanie klawiatury wirtualnej
                inputField.requestFocus()
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(inputField, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    private fun searchWords(inputLetters: String) {
        if (inputLetters.isEmpty()) {
            // Jeśli inputLetters jest pusty, nie robimy wyszukiwania
            adapter.clear()
            infoLabel.visibility = View.INVISIBLE
            return
        }
        val cleanedInputLetters = inputLetters.toLowerCase(Locale.getDefault())
        val letterCount = cleanedInputLetters.length

        runOnUiThread {
            infoLabel.text = "Szukam..."
            infoLabel.visibility = View.VISIBLE
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
        val cleanedInputLetters = inputLetters.toLowerCase(Locale.getDefault())

        runOnUiThread {
            infoLabel.text = "Szukam wszystkich słów..."
            infoLabel.visibility = View.VISIBLE
            infoLabel.setTextColor(getColor(android.R.color.holo_red_dark))
        }

        executorService.submit {
            val foundWords =
                findAllWords(database, cleanedInputLetters).sortedWith(compareByDescending<String> { it.length }.thenBy { it })
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
