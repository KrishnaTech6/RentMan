package com.rentman.sharedui.components

import com.rentman.domain.billing.SuggestMeterReadingUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MeterTextTest {
    private fun piece(text: String, left: Float, top: Float = 0.10f, bottom: Float = 0.20f) = TextPiece(text, left, top, left + 0.05f, bottom)

    @Test
    fun digitPiecesOnOneRowAreJoinedLeftToRightWhateverOrderTheyCameIn() {
        val lines = readingLines(listOf(piece("56", 0.30f), piece("6", 0.20f), piece("-6", 0.40f), piece("4", 0.50f)))

        assertTrue("65664" in lines)
        assertTrue("6566" in lines) // a run without the red tenths digit
        assertTrue("5664" in lines && "656" in lines) // every run of three or more
        assertTrue("56" in lines && "4" in lines) // each piece's own text is kept
    }

    @Test
    fun piecesOnDifferentRowsOrOfDifferentHeightsAreNotJoined() {
        val display = piece("6", 0.20f)
        val below = piece("1", 0.30f, top = 0.30f, bottom = 0.40f)
        val tiny = piece("5", 0.40f, top = 0.12f, bottom = 0.16f)

        assertEquals(listOf("6", "1", "5"), readingLines(listOf(display, below, tiny)))
    }

    @Test
    fun longerNumbersAndWordsAreLeftAsTheyAre() {
        val lines = readingLines(listOf(piece("Sr No", 0.10f), piece("804512", 0.20f), piece("3200", 0.40f)))

        assertEquals(listOf("Sr No", "804512", "3200"), lines)
    }

    @Test
    fun everyRealPhotoOfTheMeterReadsTheDisplayOnceEnlarged() {
        val suggest = SuggestMeterReadingUseCase()
        amtlMeterPhotos.forEach { photo ->
            assertEquals(6566, suggest(readingLines(photo.enlarged), previousReading = 6504).reading, photo.name)
            // Not enlarged, some are missed, but no other number is ever filled in.
            assertTrue(suggest(readingLines(photo.asIs), previousReading = 6504).reading in setOf(6566, null), photo.name)
        }
    }

    private fun CropRect.holds(x: Float, y: Float) = x in left..right && y in top..bottom

    @Test
    fun onAPhotoOfThreeMetersTheBoxStartsOnTheMeterThatFitsTheLastReading() {
        val meters = findMeters(threeMetersPhoto, lastReading = 6504)
        assertEquals(2, meters.size) // the right meter's display was not read
        val middle = meters.first()
        assertTrue(middle.display.holds(0.52f, 0.35f)) // 6566, the middle meter's display
        assertTrue(!middle.display.holds(0.53f, 0.545f)) // the reading is not looked for in the serial number
        // The kept box is the whole middle meter, with its serial number, and none of the meters next to it.
        assertTrue(middle.body.holds(0.52f, 0.35f) && middle.body.holds(0.53f, 0.545f))
        assertTrue(!middle.body.holds(0.20f, 0.31f) && !middle.body.holds(0.78f, 0.33f))

        assertTrue(findMeters(threeMetersPhoto, lastReading = 9815).first().display.holds(0.20f, 0.31f)) // 9913, left meter
    }

    @Test
    fun withNoLastReadingTheBoxStartsOnTheLeftMostMeter() {
        assertTrue(findMeters(threeMetersPhoto, lastReading = null).first().display.holds(0.20f, 0.31f))
    }

    @Test
    fun aPhotoWithNoDisplayFindsNoMeter() {
        assertEquals(emptyList(), findMeters(listOf(TextPiece("Sr No 804512", 0.1f, 0.1f, 0.3f, 0.12f), TextPiece("3200", 0.5f, 0.5f, 0.6f, 0.52f)), lastReading = 6504))
    }

    @Test
    fun aDisplayReadAsOneNumberThatFitsTheLastReadingIsAMeter() {
        val meters = findMeters(listOf(TextPiece("06566", 0.4f, 0.3f, 0.6f, 0.35f), TextPiece("13779", 0.1f, 0.2f, 0.2f, 0.22f)), lastReading = 6504)
        assertEquals(1, meters.size)
        assertTrue(meters.single().display.holds(0.5f, 0.32f))
    }

    @Test
    fun theCropOfEachAutoBoxReadsThatMetersReading() {
        val suggest = SuggestMeterReadingUseCase()
        assertEquals(6566, suggest(readingLines(middleMeterCrop), previousReading = 6504).reading)
        assertEquals(9913, suggest(readingLines(leftMeterCrop), previousReading = 9815).reading)
    }
}
