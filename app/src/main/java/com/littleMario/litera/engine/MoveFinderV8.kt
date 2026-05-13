package com.littleMario.litera.engine

class MoveFinderV8(
    private val root: Node,
    private val board: Board,
    private val anchor: Anchor,
    private val validator: MoveValidator,
    private val scoring: Scoring,
    private val crossCheck: Array<Array<Set<Char>>>
) {

    private val alphabet = "aąbcćdeęfghijklłmnńoópqrsśtuvwxyzźż"

    fun find(rack: Map<Char, Int>): List<Move> {

        val result = mutableListOf<Move>()
        val anchors = anchor.get(board)

        for ((x, y) in anchors) {

            extend(
                node = root,
                rack = rack.toMutableMap(),
                x = x,
                y = y,
                word = "",
                dir = true,
                result = result
            )

            extend(
                node = root,
                rack = rack.toMutableMap(),
                x = x,
                y = y,
                word = "",
                dir = false,
                result = result
            )
        }

        return result
            .sortedByDescending { it.score }
            .take(50)
    }

    // -------------------------------------------------
    // CORE DFS
    // -------------------------------------------------

    private fun extend(
        node: Node,
        rack: MutableMap<Char, Int>,
        x: Int,
        y: Int,
        word: String,
        dir: Boolean,
        result: MutableList<Move>
    ) {

        // 🔥 1. bounds (TYLKO raz)
        if (!board.inBounds(x, y)) return

        val fixed = board.get(x, y)

        // -------------------------------------------------
        // CASE 1: pole zajęte
        // -------------------------------------------------

        if (fixed != null) {

            val idx = alphabet.indexOf(fixed)
            if (idx < 0) return

            val next = node.next[idx] ?: return
            val (nx, ny) = nextPos(x, y, dir)

            extend(next, rack, nx, ny, word + fixed, dir, result)
            return
        }

        // -------------------------------------------------
        // CASE 2: puste pole
        // -------------------------------------------------

        val allowed = crossCheck[x][y]

        for ((ch, count) in rack.toMap()) {   // 🔥 FIX: snapshot mapy

            if (count <= 0) continue
            if (ch !in allowed) continue

            val idx = alphabet.indexOf(ch)
            if (idx < 0) continue

            val next = node.next[idx] ?: continue

            // 🔥 BACKTRACK SAFE
            rack[ch] = count - 1

            val newWord = word + ch
            val (nx, ny) = nextPos(x, y, dir)

            // -------------------------------------------------
            // TERMINAL
            // -------------------------------------------------

            if (next.terminal && newWord.length > 1) {

                if (validator.isValid(newWord, emptyList())) {

                    result.add(
                        Move(
                            word = newWord,
                            row = x,
                            col = y,
                            direction = if (dir) "H" else "V",
                            score = scoring.score(newWord, x, y, dir)
                        )
                    )
                }
            }

            // -------------------------------------------------
            // RECURSION
            // -------------------------------------------------

            if (hasFuture(next)) {
                extend(next, rack, nx, ny, newWord, dir, result)
            }

            // 🔥 restore
            rack[ch] = count
        }
    }

    // -------------------------------------------------
    // HELPERS
    // -------------------------------------------------

    private fun nextPos(x: Int, y: Int, horizontal: Boolean): Pair<Int, Int> {
        return if (horizontal) {
            x to y + 1
        } else {
            x + 1 to y
        }
    }

    private fun hasFuture(node: Node): Boolean {
        return node.next.any { it != null }
    }
}
