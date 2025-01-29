import android.os.*
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.littleMario.litera.R
import kotlinx.coroutines.*
import java.text.Collator
import java.util.*

class MainActivity : AppCompatActivity() {

    private var database: List<String> = emptyList()
    private lateinit var inputField: EditText
    private lateinit var wordList: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var infoLabel: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var searchAllButton: Button
    private lateinit var searchFromAllButton: Button

    private var lastInputLetters: String? = null
    private var searchJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val polishAlphabetComparator = Collator.getInstance(Locale("pl", "PL")).let { collator ->
        Comparator { s1: String, s2: String -> collator.compare(s1, s2) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        inputField = findViewById(R.id.inputField)
        wordList = findViewById(R.id.wordList)
        infoLabel = findViewById(R.id.infoLabel)
        progressBar = findViewById(R.id.progressBar)
        searchAllButton = findViewById(R.id.searchAllButton)
        searchFromAllButton = findViewById(R.id.searchFromAllButton)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        wordList.adapter = adapter

        loadDatabaseFromFile()

        searchAllButton.setOnClickListener { startSearch(true) }
        searchFromAllButton.setOnClickListener { startSearch(false) }
    }

    private fun startSearch(searchAll: Boolean) {
        val inputText = inputField.text.toString().lowercase(Locale.getDefault())

        if (inputText.length < 2) {
            Toast.makeText(this, "Wpisz co najmniej dwa znaki.", Toast.LENGTH_SHORT).show()
            return
        }

        if (inputText != lastInputLetters) {
            lastInputLetters = inputText
            adapter.clear()
            searchJob?.cancel()
        }

        infoLabel.text = "Szukam..."
        infoLabel.visibility = View.VISIBLE
        infoLabel.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))

        searchJob = coroutineScope.launch(Dispatchers.IO) {
            val foundWords = if (searchAll) searchAllWords(inputText) else searchWords(inputText)

            withContext(Dispatchers.Main) {
                adapter.clear()
                adapter.addAll(foundWords)
                infoLabel.text = if (foundWords.isEmpty()) "Nie znaleziono słów." else "Oto pasujące słowa."
                infoLabel.setTextColor(
                    ContextCompat.getColor(this@MainActivity, if (foundWords.isEmpty()) android.R.color.holo_red_dark else android.R.color.holo_green_dark)
                )
            }
        }
    }

    private fun searchWords(inputLetters: String): List<String> {
        return database.filter { canFormWord(it, inputLetters) }
            .sortedWith(polishAlphabetComparator)
    }

    private fun searchAllWords(inputLetters: String): List<String> {
        val inputCounter = inputLetters.groupingBy { it }.eachCount()
        return database.filter { canFormAnyWord(it, inputCounter) }
            .sortedWith(compareByDescending<String> { it.length }.thenComparing(polishAlphabetComparator))
    }

    private fun canFormWord(word: String, inputLetters: String): Boolean {
        if (word.length != inputLetters.length) return false
        val inputCounter = inputLetters.groupingBy { it }.eachCount().toMutableMap()
        return word.all { char -> inputCounter.computeIfPresent(char) { _, count -> count - 1 }?.let { it >= 0 } ?: false }
    }

    private fun canFormAnyWord(word: String, inputCounter: Map<Char, Int>): Boolean {
        val wordCounter = word.groupingBy { it }.eachCount()
        return wordCounter.all { (char, count) -> inputCounter.getOrDefault(char, 0) >= count }
    }

    private fun loadDatabaseFromFile() {
        progressBar.visibility = View.VISIBLE
        progressBar.progress = 0

        coroutineScope.launch(Dispatchers.IO) {
            val databaseList = mutableListOf<String>()
            try {
                resources.openRawResource(R.raw.words).bufferedReader().useLines { lines -> databaseList.addAll(lines) }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            withContext(Dispatchers.Main) {
                database = databaseList
                progressBar.visibility = View.GONE
                inputField.visibility = View.VISIBLE
                searchAllButton.visibility = View.VISIBLE
                searchFromAllButton.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroy() {
        searchJob?.cancel()
        coroutineScope.cancel()
        super.onDestroy()
    }
}