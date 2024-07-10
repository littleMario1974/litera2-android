package com.littleMario.litera

import android.os.Bundle
import android.os.PowerManager
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import java.io.DataInputStream
import java.io.IOException
import java.text.Collator
import java.util.*
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
    private lateinit var searchFromAllButton: Button
    private lateinit var programDescription: TextView
    private lateinit var adView: AdView
    private lateinit var webView: WebView
    private lateinit var closeButton: ImageButton
    private lateinit var closeWebViewButton: Button
    private lateinit var showDescriptionButton: Button

    private val executorService = Executors.newFixedThreadPool(4)
    private val POLISH_LETTERS = "aąbcćdeęfghijklłmnńoópqrsśtuvwxyzźż"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Mobile Ads SDK
        MobileAds.initialize(this) {}

        // Reference AdView from layout
        adView = findViewById(R.id.adView)

        // Create AdRequest object and load ad
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)

        inputField = findViewById(R.id.inputField)
        wordList = findViewById(R.id.wordList)
        infoLabel = findViewById(R.id.infoLabel)
        progressBar = findViewById(R.id.progressBar)
        clearButton = findViewById(R.id.clearButton)
        searchAllButton = findViewById(R.id.searchAllButton)
        searchFromAllButton = findViewById(R.id.searchFromAllButton)
        programDescription = findViewById(R.id.programDescription)
        webView = findViewById(R.id.webView)
        closeButton = findViewById(R.id.closeButton)
        closeWebViewButton = findViewById(R.id.closeWebViewButton)
        showDescriptionButton = findViewById(R.id.showDescriptionButton)

        // Initialize WebView
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()

        // Set background and text colors based on power save mode
        setThemeColors()

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        wordList.adapter = adapter

        // Initially hide infoLabel, progressBar, buttons ("Clear", "Search All", "Search From All"), and inputField
        infoLabel.visibility = View.INVISIBLE
        progressBar.visibility = View.GONE
        clearButton.visibility = View.GONE
        searchAllButton.visibility = View.GONE
        searchFromAllButton.visibility = View.GONE
        inputField.visibility = View.GONE

        loadDatabaseFromFile()

        inputField.apply {
            inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS

            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s != null) {
                        val newText = s.toString()

                        adapter.clear()
                        infoLabel.visibility = View.INVISIBLE

                        val numSpaces = newText.count { it == ' ' }
                        if (numSpaces > 1) {
                            Toast.makeText(
                                this@MainActivity,
                                "Dozwolona jest tylko jedna spacja.",
                                Toast.LENGTH_SHORT
                            ).show()

                            val firstSpaceIndex = newText.indexOf(' ')
                            val sanitizedText = newText.substring(0, firstSpaceIndex + 1) +
                                    newText.substring(firstSpaceIndex + 1).replace(" ", "")
                            setText(sanitizedText)
                            setSelection(length())
                            return
                        }

                        val disallowedChar = newText.find { it !in POLISH_LETTERS && it != ' ' }
                        if (disallowedChar != null) {
                            Toast.makeText(
                                this@MainActivity,
                                "Nieprawidłowy znak.",
                                Toast.LENGTH_SHORT
                            ).show()

                            val sanitizedText = newText.replace(disallowedChar.toString(), "", true)
                            setText(sanitizedText)
                            setSelection(length())
                            return
                        }
                    }
                }

                override fun afterTextChanged(s: Editable?) {}
            })
        }

        clearButton.setOnClickListener {
            inputField.text.clear()
            adapter.clear()
            infoLabel.visibility = View.INVISIBLE
        }

        searchAllButton.setOnClickListener {
            val inputText = inputField.text.toString()
            if (inputText.length >= 3) {
                searchAllWords(inputText)
            } else {
                Toast.makeText(
                    this,
                    "Wpisz co najmniej trzy znaki.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        searchFromAllButton.setOnClickListener {
            val inputText = inputField.text.toString()
            if (inputText.length >= 3) {
                searchWords(inputText)
            } else {
                Toast.makeText(
                    this,
                    "Wpisz co najmniej trzy znaki.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        wordList.setOnItemClickListener { _, _, position, _ ->
            val selectedWord = adapter.getItem(position)
            if (selectedWord != null) {
                val url = "https://sjp.pl/$selectedWord"
                webView.loadUrl(url)
                showWebView()
            }
        }

        closeWebViewButton.setOnClickListener {
            hideWebView()
        }

        showDescriptionButton.setOnClickListener {
            toggleProgramDescriptionVisibility()
        }

        closeButton.setOnClickListener {
            finish()
        }
    }

    private fun showWebView() {
        webView.visibility = View.VISIBLE
        wordList.visibility = View.GONE
        inputField.visibility = View.GONE
        clearButton.visibility = View.GONE
        searchAllButton.visibility = View.GONE
        searchFromAllButton.visibility = View.GONE
        infoLabel.visibility = View.GONE
        programDescription.visibility = View.GONE
        closeWebViewButton.visibility = View.VISIBLE
        showDescriptionButton.visibility = View.GONE // Ukryj przycisk OPIS PROGRAMU

        // Ustawianie przycisku "ZAMKNIJ" na górze po prawej stronie
        val params = closeWebViewButton.layoutParams as ConstraintLayout.LayoutParams
        params.topMargin = resources.getDimensionPixelSize(R.dimen.close_button_top_margin)
        params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
        closeWebViewButton.layoutParams = params
    }

    private fun hideWebView() {
        webView.visibility = View.GONE
        wordList.visibility = View.VISIBLE
        inputField.visibility = View.VISIBLE
        clearButton.visibility = View.VISIBLE
        searchAllButton.visibility = View.VISIBLE
        searchFromAllButton.visibility = View.VISIBLE
        if (adapter.isEmpty) {
            infoLabel.visibility = View.VISIBLE
        }
        closeWebViewButton.visibility = View.GONE
        showDescriptionButton.visibility = View.VISIBLE // Pokaż przycisk OPIS PROGRAMU
    }


    private fun toggleProgramDescriptionVisibility() {
        if (programDescription.visibility == View.VISIBLE) {
            programDescription.visibility = View.GONE
        } else {
            programDescription.visibility = View.VISIBLE
        }

        if (isPowerSaveMode()) {
            programDescription.setTextColor(ContextCompat.getColor(this, android.R.color.white))
        } else {
            programDescription.setTextColor(ContextCompat.getColor(this, android.R.color.black))
        }
    }

    private fun setThemeColors() {
        val isPowerSaveMode = isPowerSaveMode()

        if (isPowerSaveMode) {
            findViewById<ConstraintLayout>(R.id.mainLayout).setBackgroundResource(R.drawable.background_energysaver)
            inputField.setBackgroundColor(ContextCompat.getColor(this, R.color.transparent_dark))
            wordList.setBackgroundColor(ContextCompat.getColor(this, R.color.transparent_dark))
            infoLabel.setTextColor(ContextCompat.getColor(this, android.R.color.white))
        } else {
            findViewById<ConstraintLayout>(R.id.mainLayout).setBackgroundResource(R.drawable.background)
            inputField.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
            wordList.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
            infoLabel.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
        }
    }

    private fun isPowerSaveMode(): Boolean {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        return powerManager.isPowerSaveMode
    }

    private fun loadDatabaseFromFile() {
        progressBar.visibility = View.VISIBLE
        progressBar.progress = 0

        executorService.submit {
            val database = mutableListOf<String>()
            try {
                val inputStream = resources.openRawResource(R.raw.words)
                val dataInputStream = DataInputStream(inputStream)

                val fileSize = inputStream.available().toFloat()
                var totalRead = 0f
                var bytesRead = 0

                while (dataInputStream.available() > 0) {
                    val length = dataInputStream.readInt()
                    val bytes = ByteArray(length)
                    dataInputStream.read(bytes)
                    val word = String(bytes, Charsets.UTF_8)
                    database.add(word)

                    totalRead += length
                    bytesRead += length

                    if (bytesRead >= fileSize * 0.05 || dataInputStream.available() == 0) {
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
                inputField.visibility = View.VISIBLE
                clearButton.visibility = View.VISIBLE
                searchAllButton.visibility = View.VISIBLE
                searchFromAllButton.visibility = View.VISIBLE

                inputField.requestFocus()
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(inputField, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    fun getPolishAlphabetOrder(): Comparator<String> {
        val collator = Collator.getInstance(Locale("pl", "PL"))
        return Comparator { s1, s2 -> collator.compare(s1, s2) }
    }

    private fun searchWords(inputLetters: String) {
        if (inputLetters.isEmpty()) {
            adapter.clear()
            infoLabel.visibility = View.INVISIBLE
            return
        }
        val cleanedInputLetters = inputLetters.lowercase(Locale.getDefault())

        runOnUiThread {
            infoLabel.text = "Szukam..."
            infoLabel.visibility = View.VISIBLE
            infoLabel.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
        }

        executorService.submit {
            val foundWords = findWords(database, cleanedInputLetters)
                .sortedWith(getPolishAlphabetOrder())

            runOnUiThread {
                adapter.clear()
                adapter.addAll(foundWords)
                if (foundWords.isEmpty()) {
                    infoLabel.text = "Nie znaleziono słów."
                    infoLabel.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
                } else {
                    infoLabel.text = "Oto pasujące słowa."
                    infoLabel.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
                }
            }
        }
    }

    private fun searchAllWords(inputLetters: String) {
        val cleanedInputLetters = inputLetters.lowercase(Locale.getDefault())

        runOnUiThread {
            infoLabel.text = "Szukam wszystkich słów..."
            infoLabel.visibility = View.VISIBLE
            infoLabel.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
        }

        executorService.submit {
            val inputLength = cleanedInputLetters.length
            val foundWords = mutableListOf<String>()

            if (inputLength >= 3) {
                val combinations = generateCombinations(inputLetters)

                for (combination in combinations) {
                    val combinationLength = combination.length
                    if (combinationLength <= inputLength) {
                        val inputCounter = combination.groupingBy { it }.eachCount().toMutableMap()
                        for (word in database) {
                            if (word.length <= inputLength && canFormAnyWord(word, inputCounter)) {
                                foundWords.add(word)
                            }
                        }
                    }
                }
            }

            runOnUiThread {
                adapter.clear()
                adapter.addAll(foundWords.distinct().sortedWith(compareByDescending<String> { it.length }.thenComparing(getPolishAlphabetOrder())))
                if (foundWords.isEmpty()) {
                    infoLabel.text = "Nie znaleziono pasujących słów."
                    infoLabel.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
                } else {
                    infoLabel.text = "Oto wszystkie możliwe słowa."
                    infoLabel.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
                }
            }
        }
    }

    private fun generateCombinations(inputLetters: String): List<String> {
        val combinations = mutableListOf<String>()
        if (inputLetters.contains(" ")) {
            val spaceIndex = inputLetters.indexOf(" ")
            for (letter in POLISH_LETTERS) {
                val combination = inputLetters.substring(0, spaceIndex) + letter + inputLetters.substring(spaceIndex + 1)
                combinations.add(combination)
            }
        } else {
            combinations.add(inputLetters)
        }
        return combinations
    }

    private fun findWords(database: List<String>, inputLetters: String): List<String> {
        val foundWords = mutableListOf<String>()

        if (inputLetters.contains(" ")) {
            for (c in POLISH_LETTERS) {
                val inputWithReplacement = inputLetters.replaceFirst(" ", c.toString())
                foundWords.addAll(database.filter { canFormWord(it, inputWithReplacement, inputLetters.length) })
            }
        } else {
            foundWords.addAll(database.filter { canFormWord(it, inputLetters, inputLetters.length) })
        }

        return foundWords
    }

    private fun canFormWord(word: String, inputLetters: String, letterCount: Int): Boolean {
        if (word.length != letterCount) {
            return false
        }

        val inputCounter = inputLetters.groupingBy { it }.eachCount().toMutableMap()
        val wordCounter = word.groupingBy { it }.eachCount()

        for ((char, count) in wordCounter) {
            if (inputCounter.getOrDefault(char, 0) < count) {
                return false
            }
        }

        return true
    }

    private fun canFormAnyWord(word: String, inputCounter: Map<Char, Int>): Boolean {
        val wordCounter = word.groupingBy { it }.eachCount()

        for ((char, count) in wordCounter) {
            if (inputCounter.getOrDefault(char, 0) < count) {
                return false
            }
        }

        return true
    }

    override fun onDestroy() {
        adapter.clear()
        if (::database.isInitialized) {
            database.clear()
        }
        adView.destroy()
        executorService.shutdown()
        super.onDestroy()
    }

}
