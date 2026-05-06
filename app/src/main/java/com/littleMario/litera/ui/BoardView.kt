package com.littleMario.litera.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View

class BoardView(context: Context): View(context) {

    private val paint = Paint()

    override fun onDraw(canvas: Canvas) {

        val size = width / 15f

        for (y in 0 until 15) {
            for (x in 0 until 15) {

                canvas.drawRect(
                    x*size,
                    y*size,
                    (x+1)*size,
                    (y+1)*size,
                    paint
                )
            }
        }
    }
}