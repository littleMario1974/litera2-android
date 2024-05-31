package com.example.literakowanie

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {
    private lateinit var database: MutableList<String>
    private lateinit var inputField: EditText
    private lateinit var wordList: ListView
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        database = loadDatabaseFromCache() ?: loadDatabaseFromFile()

        inputField = findViewById(R.id.inputField)
        wordList = findViewById(R.id.wordList)
        val clearButton: Button = findViewById(R.id.clearButton)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        wordList.adapter = adapter

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
        }
    }

    private fun loadDatabaseFromCache(): MutableList<String>? {
        // Tu wczytaj bazę słów z pamięci podręcznej, jeśli istnieje
        return null
    }

    private fun loadDatabaseFromFile(): MutableList<String> {
        val database = mutableListOf<String>()
        try {
            val inputStream = resources.openRawResource(R.raw.words)
            val reader = BufferedReader(InputStreamReader(inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val words = line!!.split("\\s+".toRegex())
                for (word in words) {
                    database.add(word)
                }
            }
            // Zapisz bazę słów do pamięci podręcznej
            saveDatabaseToCache(database)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return database
    }

    private fun saveDatabaseToCache(database: MutableList<String>) {
        // Tu zapisz bazę słów do pamięci podręcznej
    }

    private fun searchWords(inputLetters: String) {
        val letterCount = inputLetters.length
        val foundWords = findWords(database, inputLetters, letterCount)

        adapter.clear()
        adapter.addAll(foundWords)
    }

    private fun findWords(database: List<String>, inputLetters: String, letterCount: Int): List<String> {
        val foundWords = mutableListOf<String>()
        val letters = mutableMapOf<Char, Int>()

        for (letter in inputLetters) {
            letters[letter] = letters.getOrDefault(letter, 0) + 1
        }

        for (word in database) {
            if (canFormWord(word, letters.toMutableMap(), letterCount)) {
                foundWords.add(word)
            }
        }

        return foundWords
    }

    private fun canFormWord(word: String, letters: MutableMap<Char, Int>, letterCount: Int): Boolean {
        if (word.length != letterCount) {
            return false
        }

        val wordLetters = mutableMapOf<Char, Int>()
        for (letter in word) {
            wordLetters[letter] = wordLetters.getOrDefault(letter, 0) + 1
        }

        for (letter in wordLetters.keys) {
            if (!letters.containsKey(letter) || letters[letter]!! < wordLetters[letter]!!) {
                return false
            }
        }

        return true
    }
}
