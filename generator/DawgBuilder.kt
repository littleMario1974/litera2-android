import java.io.*

class DawgBuilder {

    private val alphabet = "aąbcćdeęfghijklłmnńoópqrsśtuvwxyzźż"
    private val size = alphabet.length

    private val root = Node()

    fun build(words: List<String>) {
        words.forEach { insert(it) }
    }

    private fun insert(word: String) {

        var node = root

        for (c in word.lowercase()) {

            val i = alphabet.indexOf(c)
            if (i == -1) return

            if (node.next[i] == null) {
                node.next[i] = Node()
            }

            node = node.next[i]!!
        }

        node.terminal = true
    }

    fun save(file: File) {

        DataOutputStream(FileOutputStream(file)).use { out ->
            write(root, out)
        }
    }

    private fun write(node: Node, out: DataOutputStream) {

        out.writeBoolean(node.terminal)

        for (i in 0 until size) {
            val child = node.next[i]
            out.writeBoolean(child != null)
            if (child != null) write(child, out)
        }
    }
}

class Node(
    val next: Array<Node?> = arrayOfNulls(35),
    var terminal: Boolean = false
)