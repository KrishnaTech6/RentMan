package com.rentman.sharedui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class CropRectTest {
    private val box = CropRect(left = 0.2f, top = 0.3f, right = 0.6f, bottom = 0.5f)

    @Test
    fun theWholePhotoIsTheWholePhoto() {
        assertEquals(CropRect(0f, 0f, 1f, 1f), CropRect.Whole)
    }

    @Test
    fun movingKeepsTheSizeAndStopsAtTheEdges() {
        val moved = box.moved(dx = 0.1f, dy = 0.1f)
        assertEquals(0.4f, moved.right - moved.left, 0.0001f)
        assertEquals(0.2f, moved.bottom - moved.top, 0.0001f)

        val pushedPastTheEdge = box.moved(dx = 5f, dy = -5f)
        assertEquals(CropRect(left = 0.6f, top = 0f, right = 1f, bottom = 0.2f).left, pushedPastTheEdge.left, 0.0001f)
        assertEquals(1f, pushedPastTheEdge.right, 0.0001f)
        assertEquals(0f, pushedPastTheEdge.top, 0.0001f)
        assertEquals(0.4f, pushedPastTheEdge.right - pushedPastTheEdge.left, 0.0001f)
    }

    @Test
    fun draggingACornerMovesOnlyThatCornersEdges() {
        val dragged = box.withCornerMoved(Corner.TopLeft, dx = -0.1f, dy = -0.1f)
        assertEquals(CropRect(left = 0.1f, top = 0.2f, right = 0.6f, bottom = 0.5f).left, dragged.left, 0.0001f)
        assertEquals(0.2f, dragged.top, 0.0001f)
        assertEquals(0.6f, dragged.right, 0.0001f)
        assertEquals(0.5f, dragged.bottom, 0.0001f)

        val other = box.withCornerMoved(Corner.BottomRight, dx = 0.1f, dy = 0.2f)
        assertEquals(0.2f, other.left, 0.0001f)
        assertEquals(0.7f, other.right, 0.0001f)
        assertEquals(0.7f, other.bottom, 0.0001f)
    }

    @Test
    fun aCornerCannotLeaveThePhotoOrCrossTheOppositeSide() {
        val outside = box.withCornerMoved(Corner.TopLeft, dx = -5f, dy = -5f)
        assertEquals(0f, outside.left, 0.0001f)
        assertEquals(0f, outside.top, 0.0001f)

        val crossed = box.withCornerMoved(Corner.TopLeft, dx = 5f, dy = 5f)
        assertEquals(crossed.right - CropRect.MIN_SIZE, crossed.left, 0.0001f)
        assertEquals(crossed.bottom - CropRect.MIN_SIZE, crossed.top, 0.0001f)

        val squeezed = box.withCornerMoved(Corner.BottomRight, dx = -5f, dy = -5f)
        assertEquals(squeezed.left + CropRect.MIN_SIZE, squeezed.right, 0.0001f)
        assertEquals(squeezed.top + CropRect.MIN_SIZE, squeezed.bottom, 0.0001f)
    }

    @Test
    fun theBoxInPixelsOfAPhotoIsAtLeastOnePixelAndInsideIt() {
        val pixels = CropRect(left = 0.25f, top = 0.5f, right = 0.75f, bottom = 1f).inPixels(width = 1600, height = 1200)
        assertEquals(listOf(400, 600, 800, 600), listOf(pixels.left, pixels.top, pixels.width, pixels.height))

        val tiny = CropRect(0f, 0f, 0.0001f, 0.0001f).inPixels(width = 100, height = 100)
        assertEquals(listOf(1, 1), listOf(tiny.width, tiny.height))
    }

    @Test
    fun aBoxInsideAnotherIsGivenInThatOnesFractions() {
        val display = CropRect(left = 0.3f, top = 0.35f, right = 0.5f, bottom = 0.45f)
        val inBox = display.within(box) // box is 0.2..0.6 across, 0.3..0.5 down

        assertEquals(0.25f, inBox.left, 0.0001f)
        assertEquals(0.25f, inBox.top, 0.0001f)
        assertEquals(0.75f, inBox.right, 0.0001f)
        assertEquals(0.75f, inBox.bottom, 0.0001f)
    }

    @Test
    fun aBoxReachingOutsideAnotherIsCutAtItsEdges() {
        val inBox = CropRect(left = 0.1f, top = 0.4f, right = 0.4f, bottom = 0.6f).within(box)

        assertEquals(0f, inBox.left, 0.0001f)
        assertEquals(1f, inBox.bottom, 0.0001f)
    }

    @Test
    fun aBoxHoldsAnotherWhenThatOnesMiddleIsInsideIt() {
        assertEquals(true, box.holds(CropRect(left = 0.3f, top = 0.35f, right = 0.7f, bottom = 0.45f))) // middle 0.5, 0.4
        assertEquals(false, box.holds(CropRect(left = 0.55f, top = 0.35f, right = 0.9f, bottom = 0.45f))) // middle 0.725
    }
}
