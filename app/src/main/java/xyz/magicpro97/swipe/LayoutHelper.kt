package xyz.magicpro97.swipe

import android.graphics.Canvas
import kotlin.math.sqrt

object LayoutHelper {
    fun getPx(c: Canvas, sp: Int) = sp * sqrt(c.width.toDouble() * c.height) / 250
}