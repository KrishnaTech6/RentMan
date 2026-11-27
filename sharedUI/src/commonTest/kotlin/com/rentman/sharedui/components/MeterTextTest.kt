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
        val lines = readingLines(listOf(piece("Sr No", 0.10f), piece("706047", 0.20f), piece("3200", 0.40f)))

        assertEquals(listOf("Sr No", "706047", "3200"), lines)
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
}
