package com.littleMario.litera.engine

class Scoring(private val bonus: BonusTiles) {

    private val values = mapOf(
        'a' to 1,'e' to 1,'i' to 1,'n' to 1,'o' to 1,'r' to 1,'s' to 1,'w' to 1,'z' to 1,
        'c' to 2,'d' to 2,'k' to 2,'l' to 2,'m' to 2,'p' to 2,'t' to 2,'y' to 2
    )

    fun score(word: String, x: Int, y: Int, horizontal: Boolean): Int {

        var total = 0
        var mult = 1

        for (i in word.indices) {

            val c = word[i]
            val base = values[c] ?: 1

            val (bx, by) = if (horizontal) (x+i) to y else x to (y+i)

            when (bonus.get(bx, by)) {
                Bonus.DL -> total += base*2
                Bonus.TL -> total += base*3
                Bonus.DW -> { total += base; mult *= 2 }
                Bonus.TW -> { total += base; mult *= 3 }
                else -> total += base
            }
        }

        return total * mult
    }
}