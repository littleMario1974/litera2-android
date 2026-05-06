import java.io.File

fun main() {

    val words = File("words.txt").readLines()

    val builder = DawgBuilder()
    builder.build(words)

    builder.save(File("dictionary.dawg"))

    println("DONE")
}