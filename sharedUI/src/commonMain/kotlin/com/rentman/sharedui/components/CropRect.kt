package com.rentman.sharedui.components

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal enum class Corner { TopLeft, TopRight, BottomLeft, BottomRight }

/** A box on a photo in fractions of its width and height (0 is the left or top edge, 1 the right or bottom edge). */
internal data class CropRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val isWhole: Boolean get() = this == Whole

    /** The same box moved by [dx] and [dy] (fractions), kept the same size and inside the photo. */
    fun moved(dx: Float, dy: Float): CropRect {
        val x = (left + dx).coerceIn(0f, 1f - (right - left))
        val y = (top + dy).coerceIn(0f, 1f - (bottom - top))
        return CropRect(x, y, x + (right - left), y + (bottom - top))
    }

    /** The box with one [corner] dragged by [dx] and [dy] (fractions). It stays inside the photo and at least [MIN_SIZE] wide and high. */
    fun withCornerMoved(corner: Corner, dx: Float, dy: Float): CropRect {
        val movesLeft = corner == Corner.TopLeft || corner == Corner.BottomLeft
        val movesTop = corner == Corner.TopLeft || corner == Corner.TopRight
        return CropRect(
            left = if (movesLeft) (left + dx).coerceIn(0f, right - MIN_SIZE) else left,
            top = if (movesTop) (top + dy).coerceIn(0f, bottom - MIN_SIZE) else top,
            right = if (!movesLeft) (right + dx).coerceIn(left + MIN_SIZE, 1f) else right,
            bottom = if (!movesTop) (bottom + dy).coerceIn(top + MIN_SIZE, 1f) else bottom,
        )
    }

    /** The box on a photo that is [width] by [height] pixels: at least one pixel, and always inside the photo. */
    fun inPixels(width: Int, height: Int): PixelBox {
        val x = (left * width).roundToInt().coerceIn(0, width - 1)
        val y = (top * height).roundToInt().coerceIn(0, height - 1)
        return PixelBox(x, y, max(1, min((right * width).roundToInt(), width) - x), max(1, min((bottom * height).roundToInt(), height) - y))
    }

    companion object {
        val Whole = CropRect(0f, 0f, 1f, 1f)

        /** Small enough to pick out a meter's number, big enough to still get hold of. */
        const val MIN_SIZE = 0.1f
    }
}

internal data class PixelBox(val left: Int, val top: Int, val width: Int, val height: Int)
