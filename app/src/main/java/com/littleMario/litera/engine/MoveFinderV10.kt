package com.littleMario.litera.engine

class MoveFinderV10(

    private val root: Node,
    private val board: Board,
    private val anchor: Anchor,
    private val validator: MoveValidator,
    private val scoring: Scoring,
    private val crossCheck: Array<Array<Set<Char>>>

) {

    private val alphabet =
        "aąbcćdeęfghijklłmnńoópqrsśtuvwxyzźż"

    private val MAX_WORD = 15

    // =====================================================
    // FIND
    // =====================================================

    fun find(rack: Map<Char, Int>): List<Move> {

        val result = mutableListOf<Move>()

        val anchors =
            if (board.isEmpty())
                listOf(7 to 7)
            else
                anchor.get(board)

        for ((x, y) in anchors) {

            buildWord(x, y, true, rack, result)
            buildWord(x, y, false, rack, result)
        }

        return result
            .distinctBy {
                "${it.word}-${it.row}-${it.col}-${it.direction}"
            }
            .sortedByDescending { it.score }
            .take(500)
    }

    // =====================================================
    // BUILD
    // =====================================================

    private fun buildWord(
        startX: Int,
        startY: Int,
        horizontal: Boolean,
        rack: Map<Char, Int>,
        result: MutableList<Move>
    ) {

        val dxBack = if (horizontal) 0 else -1
        val dyBack = if (horizontal) -1 else 0

        var x = startX
        var y = startY

        // 🔥 cofnij po istniejących literach
        while (
            board.inBounds(x + dxBack, y + dyBack) &&
            board.get(x + dxBack, y + dyBack) != null
        ) {
            x += dxBack
            y += dyBack
        }

        // 🔥 dodatkowy sliding window
        for (shift in 0..7) {

            val sx =
                if (horizontal) x
                else x - shift

            val sy =
                if (horizontal) y - shift
                else y

            if (!board.inBounds(sx, sy))
                continue

            extend(
                node = root,
                rack = rack.toMutableMap(),
                x = sx,
                y = sy,
                startX = sx,
                startY = sy,
                horizontal = horizontal,
                word = "",
                usedBoardTile = false,
                segmentCount = 0,
                depth = 0,
                result = result
            )
        }
    }

    // =====================================================
    // DFS CORE
    // =====================================================

    private fun extend(
        node: Node,
        rack: MutableMap<Char, Int>,
        x: Int,
        y: Int,
        startX: Int,
        startY: Int,
        horizontal: Boolean,
        word: String,
        usedBoardTile: Boolean,
        segmentCount: Int,
        depth: Int,
        result: MutableList<Move>
    ) {

        if (depth > MAX_WORD)
            return

        // =================================================
        // END OF BOARD
        // =================================================

        if (!board.inBounds(x, y)) {

            tryAddMove(
                node,
                word,
                startX,
                startY,
                horizontal,
                usedBoardTile,
                result
            )

            return
        }

        val fixed = board.get(x, y)

        // =================================================
        // CASE 1 -> BOARD LETTER
        // =================================================

        if (fixed != null) {

            val idx = alphabet.indexOf(fixed)

            if (idx < 0)
                return

            val next =
                node.next.getOrNull(idx)
                    ?: return

            val newWord = word + fixed

            tryAddMove(
                next,
                newWord,
                startX,
                startY,
                horizontal,
                true,
                result
            )

            extend(
                node = next,
                rack = rack,

                x =
                    if (horizontal) x
                    else x + 1,

                y =
                    if (horizontal) y + 1
                    else y,

                startX = startX,
                startY = startY,

                horizontal = horizontal,

                word = newWord,

                usedBoardTile = true,

                // 🔥 segment boardowy
                segmentCount = segmentCount,

                depth = depth + 1,

                result = result
            )

            return
        }

        // =================================================
        // CASE 2 -> EMPTY
        // =================================================

        val allowed =
            crossCheck[x][y]
                .ifEmpty { alphabet.toSet() }

        for ((ch, count) in rack.toMap()) {

            if (count <= 0)
                continue

            if (ch !in allowed)
                continue

            val idx = alphabet.indexOf(ch)

            if (idx < 0)
                continue

            val next =
                node.next.getOrNull(idx)
                    ?: continue

            rack[ch] = count - 1

            val newWord = word + ch

            val touches =
                usedBoardTile ||
                        boardHasAdjacentTile(x, y)

            // 🔥 multi-segment logic
            val newSegmentCount =
                if (touches)
                    segmentCount
                else
                    segmentCount + 1

            // 🔥 limit segmentów
            if (newSegmentCount > 4) {

                rack[ch] = count
                continue
            }

            tryAddMove(
                next,
                newWord,
                startX,
                startY,
                horizontal,
                touches,
                result
            )

            extend(
                node = next,
                rack = rack,

                x =
                    if (horizontal) x
                    else x + 1,

                y =
                    if (horizontal) y + 1
                    else y,

                startX = startX,
                startY = startY,

                horizontal = horizontal,

                word = newWord,

                usedBoardTile = touches,

                segmentCount = newSegmentCount,

                depth = depth + 1,

                result = result
            )

            rack[ch] = count
        }
    }

    // =====================================================
    // ADD MOVE
    // =====================================================

    private fun tryAddMove(
        node: Node,
        word: String,
        startX: Int,
        startY: Int,
        horizontal: Boolean,
        usedBoardTile: Boolean,
        result: MutableList<Move>
    ) {

        if (!node.terminal)
            return

        if (word.length < 2)
            return

        if (!usedBoardTile && !board.isEmpty())
            return

        val placements =
            buildPlacements(
                startX,
                startY,
                horizontal,
                word
            )

        if (placements.isEmpty())
            return

        if (!isValidFullBoard(placements))
            return

        result.add(
            Move(
                word = word,

                row = startX,
                col = startY,

                direction =
                    if (horizontal) "H"
                    else "V",

                score = scoring.score(
                    word,
                    startX,
                    startY,
                    horizontal
                ),

                placements = placements
            )
        )
    }

    // =====================================================
    // TOUCH CHECK
    // =====================================================

    private fun boardHasAdjacentTile(
        x: Int,
        y: Int
    ): Boolean {

        val dirs = arrayOf(
            -1 to 0,
            1 to 0,
            0 to -1,
            0 to 1
        )

        for ((dx, dy) in dirs) {

            val nx = x + dx
            val ny = y + dy

            if (
                board.inBounds(nx, ny) &&
                board.get(nx, ny) != null
            ) {
                return true
            }
        }

        return false
    }

    // =====================================================
    // BUILD PLACEMENTS
    // =====================================================

    private fun buildPlacements(
        x: Int,
        y: Int,
        horizontal: Boolean,
        word: String
    ): List<Placement> {

        val placements =
            mutableListOf<Placement>()

        for (i in word.indices) {

            val xx =
                if (horizontal) x
                else x + i

            val yy =
                if (horizontal) y + i
                else y

            if (!board.inBounds(xx, yy))
                return emptyList()

            val existing =
                board.get(xx, yy)

            if (existing == null) {

                placements.add(
                    Placement(
                        xx,
                        yy,
                        word[i]
                    )
                )

            } else if (existing != word[i]) {

                return emptyList()
            }
        }

        return placements
    }

    // =====================================================
    // VALIDATION
    // =====================================================

    private fun isValidFullBoard(
        placements: List<Placement>
    ): Boolean {

        return true
    }
}