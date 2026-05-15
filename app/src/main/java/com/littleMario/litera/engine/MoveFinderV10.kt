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

    // -------------------------------------------------
    // FIND
    // -------------------------------------------------

    fun find(rack: Map<Char, Int>): List<Move> {

        val result = mutableListOf<Move>()
        val anchors = anchor.get(board)

        if (board.isEmpty()) {
            // pierwszy ruch musi przechodzić przez środek
            buildWord(7, 7, true, rack, result)
            buildWord(7, 7, false, rack, result)
        } else {
            for ((x, y) in anchors) {
                buildWord(x, y, true, rack, result)
                buildWord(x, y, false, rack, result)
            }
        }

        return result
            .distinctBy { "${it.word}-${it.row}-${it.col}-${it.direction}" }
            .sortedByDescending { it.score }
            .take(200)
    }

    // -------------------------------------------------
    // BUILD WORD (FIX: poprawne cofanie startu)
    // -------------------------------------------------

    private fun buildWord(
        startX: Int,
        startY: Int,
        horizontal: Boolean,
        rack: Map<Char, Int>,
        result: MutableList<Move>
    ) {

        var x = startX
        var y = startY

        // 🔥 cofnij się po istniejących literach (KLUCZ DO "martwica")
        while (board.inBounds(x, y) && board.get(x, y) != null) {
            if (horizontal) y-- else x--
        }

        if (horizontal) y++ else x++

        extend(
            node = root,
            rack = rack.toMutableMap(),
            x = x,
            y = y,
            startX = x,
            startY = y,
            horizontal = horizontal,
            word = "",
            usedBoardTile = false,
            result = result
        )
    }

    // -------------------------------------------------
    // DFS CORE
    // -------------------------------------------------

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
        result: MutableList<Move>
    ) {

        if (!board.inBounds(x, y)) {

            if (node.terminal && word.length > 1 && usedBoardTile) {

                val placements = buildPlacements(startX, startY, horizontal, word)

                if (placements.isNotEmpty() && isValidFullBoard(placements)) {
                    addMove(word, startX, startY, horizontal, placements, result)
                }
            }

            return
        }

        val fixed = board.get(x, y)

        // -------------------------------------------------
        // CASE 1: istniejąca litera
        // -------------------------------------------------

        if (fixed != null) {

            val idx = alphabet.indexOf(fixed)
            if (idx < 0) return

            val next = node.next.getOrNull(idx) ?: return

            val newWord = word + fixed

            if (next.terminal && newWord.length > 1) {
                val placements = buildPlacements(startX, startY, horizontal, newWord)

                if (placements.isNotEmpty() && isValidFullBoard(placements)) {
                    addMove(newWord, startX, startY, horizontal, placements, result)
                }
            }

            val (nx, ny) = nextPos(x, y, horizontal)

            extend(
                node = next,
                rack = rack,
                x = nx,
                y = ny,
                startX = startX,
                startY = startY,
                horizontal = horizontal,
                word = newWord,
                usedBoardTile = true,
                result = result
            )

            return
        }

        // -------------------------------------------------
        // CASE 2: puste pole
        // -------------------------------------------------

        val allowed = crossCheck[x][y]

        for ((ch, count) in rack.toMap()) {

            if (count <= 0) continue
            if (ch !in allowed) continue

            val idx = alphabet.indexOf(ch)
            val next = node.next.getOrNull(idx) ?: continue

            rack[ch] = count - 1

            val newWord = word + ch

            val touches = usedBoardTile || boardHasAdjacentTile(x, y)

            if (next.terminal && newWord.length > 1 && touches) {

                val placements = buildPlacements(startX, startY, horizontal, newWord)

                if (placements.isNotEmpty() && isValidFullBoard(placements)) {
                    addMove(newWord, startX, startY, horizontal, placements, result)
                }
            }

            val (nx, ny) = nextPos(x, y, horizontal)

            extend(
                node = next,
                rack = rack,
                x = nx,
                y = ny,
                startX = startX,
                startY = startY,
                horizontal = horizontal,
                word = newWord,
                usedBoardTile = touches,
                result = result
            )

            rack[ch] = count
        }
    }

    // -------------------------------------------------
    // TOUCH CHECK
    // -------------------------------------------------

    private fun boardHasAdjacentTile(x: Int, y: Int): Boolean {
        val dirs = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)

        for ((dx, dy) in dirs) {
            val nx = x + dx
            val ny = y + dy

            if (board.inBounds(nx, ny) && board.get(nx, ny) != null) {
                return true
            }
        }

        return false
    }

    // -------------------------------------------------
    // PLACEMENTS
    // -------------------------------------------------

    private fun buildPlacements(
        x: Int,
        y: Int,
        horizontal: Boolean,
        word: String
    ): List<Placement> {

        val list = mutableListOf<Placement>()

        for (i in word.indices) {

            val xx = if (horizontal) x else x + i
            val yy = if (horizontal) y + i else y

            if (!board.inBounds(xx, yy)) return emptyList()

            val existing = board.get(xx, yy)

            if (existing == null) {
                list.add(Placement(xx, yy, word[i]))
            } else if (existing != word[i]) {
                return emptyList()
            }
        }

        return list
    }

    // -------------------------------------------------
    // FULL SCRABBLE VALIDATION (KLUCZ)
    // -------------------------------------------------

    private fun isValidFullBoard(placements: List<Placement>): Boolean {

        val temp = Array(15) { Array<Char?>(15) { null } }

        // kopiuj planszę
        for (x in 0 until 15)
            for (y in 0 until 15)
                temp[x][y] = board.get(x, y)

        // dodaj ruch
        for (p in placements) {
            temp[p.x][p.y] = p.ch
        }

        // sprawdź wszystkie słowa
        val words = extractWords(temp)

        return words.all { validator.isValid(it, emptyList()) }
    }

    // -------------------------------------------------
    // WORD EXTRACTION
    // -------------------------------------------------

    private fun extractWords(b: Array<Array<Char?>>): List<String> {

        val out = mutableListOf<String>()

        for (x in 0 until 15) {
            var w = ""
            for (y in 0 until 15) {
                val c = b[x][y]
                if (c != null) w += c
                else {
                    if (w.length > 1) out.add(w)
                    w = ""
                }
            }
            if (w.length > 1) out.add(w)
        }

        for (y in 0 until 15) {
            var w = ""
            for (x in 0 until 15) {
                val c = b[x][y]
                if (c != null) w += c
                else {
                    if (w.length > 1) out.add(w)
                    w = ""
                }
            }
            if (w.length > 1) out.add(w)
        }

        return out
    }

    // -------------------------------------------------
    // ADD MOVE
    // -------------------------------------------------

    private fun addMove(
        word: String,
        x: Int,
        y: Int,
        horizontal: Boolean,
        placements: List<Placement>,
        result: MutableList<Move>
    ) {

        result.add(
            Move(
                word = word,
                row = x,
                col = y,
                direction = if (horizontal) "H" else "V",
                score = scoring.score(word, x, y, horizontal),
                placements = placements
            )
        )
    }

    // -------------------------------------------------
    // NEXT POS
    // -------------------------------------------------

    private fun nextPos(x: Int, y: Int, horizontal: Boolean) =
        if (horizontal) x to y + 1 else x + 1 to y
}