package com.littleMario.litera.engine

class MoveFinderV6(
    private val root: Node,
    private val board: Board,
    private val anchor: Anchor,
    private val validator: MoveValidator,
    private val scoring: Scoring
) {

    private val alphabet = "aąbcćdeęfghijklłmnńoópqrsśtuvwxyzźż"

    fun find(rack: Map<Char, Int>): List<Move> {

        val result = ArrayList<Move>()

        for ((x,y) in anchor.get(board)) {
            dfs(root,"",rack.toMutableMap(),x,y,true,result)
        }

        return result.sortedByDescending { it.score }.take(100)
    }

    private fun dfs(
        node: Node,
        word: String,
        rack: MutableMap<Char, Int>,
        x: Int,
        y: Int,
        horizontal: Boolean,
        result: MutableList<Move>
    ) {

        if (node.terminal && word.isNotEmpty()) {

            val cross = emptyList<CrossWord>()

            if (!validator.isValid(word, emptyList())) return

            val score = scoring.score(word,x,y,horizontal)

            result.add(Move(word,x,y,horizontal,score,cross))
        }

        for (i in alphabet.indices) {

            val child = node.next[i] ?: continue
            val c = alphabet[i]

            val count = rack[c]

            if (count!=null && count>0) {

                rack[c] = count-1

                dfs(child,word+c,rack,x,y,horizontal,result)

                rack[c] = count
            }
        }
    }
}