package com.example.literakowanie

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var database: MutableList<String>
    private lateinit var inputField: EditText
    private lateinit var wordList: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var infoLabel: TextView

    private val executorService = Executors.newFixedThreadPool(4)
    private val POLISH_LETTERS = "aąbcćdeęfghijklłmnńoópqrsśtuvwxyzźż"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        database = loadDatabaseFromCache() ?: loadDatabaseFromFile()

        inputField = findViewById(R.id.inputField)
        wordList = findViewById(R.id.wordList)
        infoLabel = findViewById(R.id.infoLabel)
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
            infoLabel.text = ""
            infoLabel.setTextColor(getColor(android.R.color.holo_green_dark))
        }
    }

    private fun loadDatabaseFromCache(): MutableList<String>? {
        val sharedPreferences = getPreferences(Context.MODE_PRIVATE)
        val serializedDatabase = sharedPreferences.getString("database", null) ?: return null
        return serializedDatabase.split(",").toMutableList()
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
            saveDatabaseToCache(database)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return database
    }

    private fun saveDatabaseToCache(database: MutableList<String>) {
        val sharedPreferences = getPreferences(Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val serializedDatabase = database.joinToString(",")
        editor.putString("database", serializedDatabase)
        editor.apply()
    }

    private fun searchWords(inputLetters: String) {
        val cleanedInputLetters = inputLetters.toLowerCase(Locale.getDefault()).replace("[^aąbcćdeęfghijklłmnńoópqrsśtuvwxyzźż\\s]".toRegex(), "")
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

