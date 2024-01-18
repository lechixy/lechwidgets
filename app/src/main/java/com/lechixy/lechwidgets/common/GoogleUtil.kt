package com.lechixy.lechwidgets.common

import android.graphics.Bitmap
import android.graphics.Color

class GoogleUtil {

    object ThemeColors {
        val AUTO = "Auto"
        val LIGHT = "Light"
        val DARK = "Dark"
    }

    object Colors {
        val LIGHT = Color.argb(204, 255, 255, 255)
        val DARK = Color.argb(204, 0, 0, 0)
    }

    companion object {

        fun findAllThisColoredPixelsAndClear(color: Int, bitmap: Bitmap): Bitmap{
            val width = bitmap.width
            val height = bitmap.height

            // loop going from left to right and top to bottom
            for (w in 0 until width) {
                for (j in 0 until height) {
                    if (bitmap.getPixel(w, j) == color) {
                        bitmap.setPixel(w, j, Color.TRANSPARENT) // set any wanted pixels to transparent
                    }
                }
            }

            return bitmap
        }
    }
}