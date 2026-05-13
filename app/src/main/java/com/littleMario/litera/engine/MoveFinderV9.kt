package com.littleMario.litera.engine

class MoveFinderV9(
    private val root: Node,
    private val board: Board,
    private val anchor: Anchor,
    private val validator: MoveValidator,
    private val scoring: Scoring,
    private val crossCheck: Array<Array<Set<Char>>>
) {

    private val alphabet =
        "aąbcćdeęfghijklłmnńoópqrsśtuvwxyzźż"

    fun find(rack: Map<Char, Int>): List<Move> {

        val result = mutableListOf<Move>()

        val anchors = anchor.get(board)

        println("ANCHORS = $anchors")

        for ((x, y) in anchors) {

            // poziomo
            extend(
                node = root,
                rack = rack.toMutableMap(),
                startX = x,
                startY = y,
                x = x,
                y = y,
                horizontal = true,
                word = "",
                usedBoardTile = false,
                result = result
            )

            // pionowo
            extend(
                node = root,
                rack = rack.toMutableMap(),
                startX = x,
                startY = y,
                x = x,
                y = y,
                horizontal = false,
                word = "",
                usedBoardTile = false,
                result = result
            )
        }

        return result
            .distinctBy {
                "${it.word}-${it.row}-${it.col}-${it.direction}"
            }
            .sortedByDescending { it.score }
            .take(100)
    }

    // -------------------------------------------------
    // DFS
    // -------------------------------------------------

    private fun extend(
        node: Node,
        rack: MutableMap<Char, Int>,
        startX: Int,
        startY: Int,
        x: Int,
        y: Int,
        horizontal: Boolean,
        word: String,
        usedBoardTile: Boolean,
        result: MutableList<Move>
    ) {

        if (!board.inBounds(x, y)) {

            // koniec planszy → można zakończyć słowo
            if (
                node.terminal &&
                word.length > 1 &&
                usedBoardTile
            ) {

                addMove(
                    word,
                    startX,
                    startY,
                    horizontal,
                    result
                )
            }

            return
        }

        val fixed = board.get(x, y)

        // -------------------------------------------------
        // LITERA JUŻ NA PLANSZY
        // -------------------------------------------------

        if (fixed != null) {

            val idx = alphabet.indexOf(fixed)

            if (idx < 0) return

            val next = node.next[idx] ?: return

            val newWord = word + fixed

            val (nx, ny) =
                nextPos(x, y, horizontal)

            extend(
                node = next,
                rack = rack,
                startX = startX,
                startY = startY,
                x = nx,
                y = ny,
                horizontal = horizontal,
                word = newWord,
                usedBoardTile = true,
                result = result
            )

            return
        }

        // -------------------------------------------------
        // PUSTE POLE
        // -------------------------------------------------

        val allowed = crossCheck[x][y]

        for ((ch, count) in rack.toMap()) {

            if (count <= 0) continue
            if (ch !in allowed) continue

            val idx = alphabet.indexOf(ch)

            if (idx < 0) continue

            val next = node.next[idx] ?: continue

            rack[ch] = count - 1

            val newWord = word + ch

            // -------------------------------------------------
            // TERMINAL
            // -------------------------------------------------

            if (
                next.terminal &&
                newWord.length > 1 &&
                touchesBoard(
                    startX,
                    startY,
                    horizontal,
                    newWord
                )
            ) {

                addMove(
                    newWord,
                    startX,
                    startY,
                    horizontal,
                    result
                )
            }

            val (nx, ny) =
                nextPos(x, y, horizontal)

            if (hasFuture(next)) {

                extend(
                    node = next,
                    rack = rack,
                    startX = startX,
                    startY = startY,
                    x = nx,
                    y = ny,
                    horizontal = horizontal,
                    word = newWord,
                    usedBoardTile = usedBoardTile,
                    result = result
                )
            }

            rack[ch] = count
        }
    }

    // -------------------------------------------------
    // ADD MOVE
    // -------------------------------------------------

    private fun addMove(
        word: String,
        x: Int,
        y: Int,
        horizontal: Boolean,
        result: MutableList<Move>
    ) {

        if (!validator.isValid(word, emptyList())) {
            return
        }

        result.add(
            Move(
                word = word,
                row = x,
                col = y,
                direction =
                    if (horizontal) "H" else "V",

                score = scoring.score(
                    word,
                    x,
                    y,
                    horizontal
                )
            )
        )
    }

    // -------------------------------------------------
    // BOARD CONTACT
    // -------------------------------------------------

    private fun touchesBoard(
        x: Int,
        y: Int,
        horizontal: Boolean,
        word: String
    ): Boolean {

        if (board.isEmpty()) return true

        for (i in word.indices) {

            val xx =
                if (horizontal) x else x + i

            val yy =
                if (horizontal) y + i else y

            if (board.get(xx, yy) != null) {
                return true
            }
        }

        return false
    }

    // -------------------------------------------------
    // NEXT POSITION
    // -------------------------------------------------

    private fun nextPos(
        x: Int,
        y: Int,
        horizontal: Boolean
    ): Pair<Int, Int> {

        return if (horizontal) {
            x to y + 1
        } else {
            x + 1 to y
        }
    }

    // -------------------------------------------------
    // HAS CHILDREN
    // -------------------------------------------------

    private fun hasFuture(node: Node): Boolean {
        return node.next.any { it != null }
    }
}