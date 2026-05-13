package com.littleMario.litera.ui

import androidx.compose.ui.graphics.Color

fun getBonusColor(r: Int, c: Int): Color {
    return when {
        r == 7 && c == 7 -> Color(0xFFFFD54F)
        (r == c) || (r + c == 14) -> Color(0xFFEF9A9A)
        (r % 4 == 0 && c % 4 == 0) -> Color(0xFF90CAF9)
        (r == 0 || c == 0 || r == 14 || c == 14) -> Color(0xFFA5D6A7)
        else -> Color(0xFF2E7D32)
    }
}